@file:Suppress("NonAsciiCharacters")

package com.wsr.k.a.row.shared.model

import com.wsr.k.a.row.shared.domain.Board
import com.wsr.k.a.row.shared.domain.Piece
import com.wsr.k.a.row.shared.model.dqn.DQNInputConverter
import com.wsr.knist.batch.Batch
import com.wsr.knist.batch.get
import com.wsr.knist.batch.shape.toBatch
import com.wsr.knist.batch.shape.toList
import com.wsr.knist.core.IOType
import com.wsr.knist.core.d1
import com.wsr.knist.core.get
import com.wsr.knist.core.reduction.maxIndex
import com.wsr.knist.core.set
import com.wsr.knist.core.unwrap
import com.wsr.knist.network.Network
import com.wsr.knist.network.NetworkSerializer
import com.wsr.knist.network.create
import com.wsr.knist.network.initializer.He
import com.wsr.knist.network.optimizer.Scheduler
import com.wsr.knist.network.optimizer.adam.AdamW
import com.wsr.knist.network.output.mean.meanSquare
import com.wsr.knist.network.port
import com.wsr.knist.network.process.compute.affine.affine
import com.wsr.knist.network.process.compute.bias.d1.bias
import com.wsr.knist.network.process.compute.bias.d3.bias
import com.wsr.knist.network.process.compute.conv.convD2
import com.wsr.knist.network.process.compute.function.relu.swish
import com.wsr.knist.network.process.reshape.reshape.reshapeToD1
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test

private const val COL = 8
private const val ROW = 8
private const val K = 4
private val TURN = Piece.WHITE

private const val TRAIN_COUNT = 10000
private const val BATCH_SIZE = 32
private const val BUFFER_CAPACITY = 5012

private const val GAMMA = 0.9f
private const val EPSILON = 0.1f

private const val OUTPUT_FILE = "dqn.cbor"

class DQNModelTest {
    val network = run {
        NetworkSerializer.register(DQNInputConverter::class)
        Network.create(
            port = port(DQNInputConverter(col = COL, row = ROW, turn = TURN)),
            optimizer = AdamW(Scheduler.Fix(0.0001f)),
            initializer = He(),
        ) { input ->
            input
                .convD2(filter = 16, kernel = 3).bias().swish()
                .convD2(filter = 32, kernel = 5).bias().swish()
                .reshapeToD1()
                .affine(128).bias().swish()
                .affine(COL * ROW)
                .meanSquare()
        }
    }

    @Test
    fun Q学習を実行する() {
        runBlocking {
            DQNAgent(
                network = network,
                opponent = { board, seed -> board.selectRandom(seed)!! },
            )
                .train(count = TRAIN_COUNT, epsilon = { EPSILON })

            repeat(5) { version ->
                val opponent = network
                    .clone()
                    .replaceSource(
                        converter = DQNInputConverter(
                            col = COL,
                            row = ROW,
                            turn = if (TURN == Piece.WHITE) Piece.BLACK else Piece.WHITE,
                        ),
                    )
                DQNAgent(
                    network = network,
                    opponent = { board, _ ->
                        runBlocking {
                            val expect = opponent.expect(listOf(board))[0]
                            board.select(expect)
                        }
                    },
                )
                    .train(count = TRAIN_COUNT, epsilon = { EPSILON / (version + 2) })
            }
        }
    }

    @AfterTest
    fun networkを保存() {
        FileSystem.SYSTEM.sink(OUTPUT_FILE.toPath()).buffer().use {
            network.toCbor(sink = it)
        }
    }
}

// network -> 学習対象
// opponent -> 対戦相手(決定的である必要がある)
class DQNAgent(
    val network: Network.Src1.Sink1<List<Board>, Batch<IOType.D1>>,
    val opponent: (board: Board, seed: Int) -> Pair<Int, Int>,
) {
    private var targetNetwork = network.clone()
    private val buffer: ArrayDeque<Pair<Board, suspend (IOType.D1) -> IOType.D1>> = ArrayDeque()

    fun train(count: Int, epsilon: (i: Int) -> Float) = runBlocking {
        val losses = mutableListOf<Float>()
        repeat(count) { times ->
            val ep = epsilon(times)
            val board = Board(col = COL, row = ROW, k = K)
            if (TURN == Piece.WHITE) {
                val (i, j) = board.selectRandom()!!
                board[i, j] = Piece.BLACK
            }
            while (board.winner == null) {
                // 予測を元に学習データを作成する
                val expect = network.expect(listOf(board))[0]
                val (i, j) = if (Random.nextDouble(0.0, 1.0) >= ep) {
                    board.select(expect)
                } else {
                    board.selectRandom() ?: continue
                }
                if (board[i, j] != null) continue

                // Bufferにつめて後の学習に利用
                val seed = Random.nextInt()
                board.copy().also { board ->
                    buffer.addLast(board to { it.calcLabel(board.copy(), i, j, TURN, seed) })
                }
                // boardの状態を進めるための処理
                expect.calcLabel(board, i, j, TURN, seed)

                if (buffer.size <= BATCH_SIZE) continue
                if (BUFFER_CAPACITY <= buffer.size) buffer.removeFirst()

                // 学習フェーズ
                val trainData = buffer.drop(1).shuffled().take(BATCH_SIZE) + buffer.last()
                val loss = network.train(
                    input = trainData.map { (input, _) -> input },
                    label = { exp ->
                        exp.toList()
                            .zip(trainData.map { (_, calcLabel) -> calcLabel })
                            .map { (exp, calcLabel) -> runBlocking { calcLabel(exp) } }
                            .toBatch()
                    },
                )
                losses.add(loss.unwrap())
            }
            if (times % 100 == 0) {
                println("times: $times, loss: ${losses.average()}")
                losses.removeAll { true }
                targetNetwork = network.clone()
            }
        }
    }

    private suspend fun IOType.D1.calcLabel(
        board: Board,
        i: Int,
        j: Int,
        turn: Piece,
        seed: Int,
    ): IOType.D1 {
        val label = evaluate(board, i, j, turn, seed)
        val value = value.toFloatArray().also { it[i * board.row + j] = label }
        return IOType.d1(value)
    }

    // boardに副作用を起こす
    private suspend fun evaluate(board: Board, i: Int, j: Int, turn: Piece, seed: Int): Float {
        if (board.winner != null) return 0f

        // 勝利 -> 1f
        board[i, j] = turn
        if (board.winner != null) return 1f

        // 敗北 -> -1f
        val (i, j) = opponent(board, seed)
        board[i, j] = if (turn == Piece.WHITE) Piece.BLACK else Piece.WHITE
        if (board.winner != null) return -1f

        // 途中 -> 'Q
        val q = targetNetwork.expect(listOf(board))[0]
            .let { expect ->
                val (i, j) = board.select(expect)
                expect[i * board.row + j]
            }
            .unwrap()
        return GAMMA * q
    }
}

private fun Board.selectRandom(seed: Int? = null): Pair<Int, Int>? {
    if (winner != null) return null
    val placeable = (0 until col).flatMap { i ->
        (0 until row)
            .filter { j -> this[i, j] == null }
            .map { j -> i to j }
    }
    return placeable.randomOrNull(seed?.let { Random(it) } ?: Random)
}

// 候補手から実際に打てる手を選ぶ
private fun Board.select(candidate: IOType.D1): Pair<Int, Int> {
    val expect = IOType.d1(candidate.value.toFloatArray())
    repeat(col * row) {
        val maxIndex = expect.maxIndex().unwrap().roundToInt()
        val (i, j) = maxIndex / row to maxIndex % row
        if (this[i, j] == null) return i to j
        expect[i * row + j] = -Float.MAX_VALUE
    }
    error("invalid board. $this")
}
