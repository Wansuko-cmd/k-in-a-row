@file:Suppress("NonAsciiCharacters")

package com.wsr.k.a.row.shared.model.phase2

import com.wsr.k.a.row.shared.domain.Board
import com.wsr.k.a.row.shared.domain.Piece
import com.wsr.k.a.row.shared.model.dqn.DQNInputConverter
import com.wsr.knist.batch.Batch
import com.wsr.knist.batch.d1
import com.wsr.knist.batch.get
import com.wsr.knist.batch.i
import com.wsr.knist.core.IOType
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

private const val TRAIN_COUNT = 1000
private const val BATCH_SIZE = 32

private const val GAMMA = 0.9f
private const val EPSILON = 0.1f

private const val HANDS = 5

private const val OUTPUT_FILE = "dqn.cbor"

// DQN + 貪欲法
class Phase2Test {
    private val network = run {
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
            repeat(TRAIN_COUNT) {
                DQNAgent(
                    network = network,
                    opponent = { board -> board.selectRandom() },
                )
                    .train(count = 100, epsilon = { EPSILON })

                if (it % 100 == 0) println("times: $it")
            }

            repeat(TRAIN_COUNT) { version ->
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
                    opponent = { boards ->
                        runBlocking {
                            val expect = opponent.expect(boards)
                            boards.select(expect)
                        }
                    },
                )
                    .train(count = 100, epsilon = { EPSILON / (version + 2) })

                if (version % 100 == 0) println("times: $version")
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
// opponent -> 対戦相手
private class DQNAgent(
    val network: Network.Src1.Sink1<List<Board>, Batch<IOType.D1>>,
    val opponent: (boards: List<Board>) -> List<Pair<Int, Int>>,
    val batchSize: Int = BATCH_SIZE,
) {
    private val buffer: ArrayDeque<BufferData> = ArrayDeque()

    fun train(count: Int, epsilon: (i: Int) -> Float) = runBlocking {
        var boards = List(batchSize) { createBoard() }
        repeat(count) { times ->
            val ep = epsilon(times)

            // 予測を元に学習データを作成する
            val coordinates = if (Random.nextDouble(0.0, 1.0) >= ep) {
                val expect = network.expect(boards)
                boards.select(expect)
            } else {
                boards.selectRandom()
            }

            val data = evaluate(boards, coordinates, TURN)
            buffer.addAll(data)
            boards = data.map { data ->
                when (data) {
                    is BufferData.Finish -> createBoard()
                    is BufferData.Continue -> data.next
                }
            }
        }

        // 学習フェーズ
        network.train(
            input = buffer.map { it.current },
            label = { exp -> runBlocking { exp.calcLabel(buffer) } },
        )
    }

    private fun createBoard(): Board {
        val board = Board(col = COL, row = ROW, k = K)
        if (TURN == Piece.WHITE) {
            val (i, j) = opponent(listOf(board))[0]
            board[i, j] = Piece.BLACK
        }
        return board
    }

    private suspend fun Batch<IOType.D1>.calcLabel(data: List<BufferData>): Batch<IOType.D1> {
        val result = value.toFloatArray().clone()

        val input = data.filterIsInstance<BufferData.Continue>().map { it.next }
        val expect = network.clone().expect(input = input)
        val action = input.select(expect)
        var count = 0
        repeat(data.size) {
            val data = data[it]
            val label = when (data) {
                is BufferData.Finish -> if (data.isWinner) 1f else -1f
                is BufferData.Continue -> {
                    val (i, j) = action[count]
                    val expect = expect[count]
                    count++
                    GAMMA * expect[i * data.next.row + j].unwrap()
                }
            }
            val (i, j) = data.coordinate
            result[(it * data.current.col + i) * data.current.row + j] = label
        }
        return Batch.d1(size = size, i = i, value = result)
    }

    private fun evaluate(
        boards: List<Board>,
        coordinates: List<Pair<Int, Int>>,
        turn: Piece,
    ): List<BufferData> {
        check(boards.all { it.winner == null })
        val coordinates = coordinates.toMutableList()
        val current = boards.map { it.copy() }.toMutableList()
        val next = boards.map { it.copy() }.toMutableList()
        val bufferData = mutableListOf<BufferData>()
        val removed = mutableListOf<Int>()

        coordinates.forEachIndexed { index, (i, j) ->
            next[index][i, j] = turn
            if (next[index].winner != null) {
                val data = BufferData.Finish(
                    current = current[index],
                    coordinate = i to j,
                    isWinner = true,
                )
                bufferData.add(data)
                removed.add(index)
            }
        }
        removed.reversed().forEach {
            coordinates.removeAt(it)
            current.removeAt(it)
            next.removeAt(it)
        }
        removed.clear()

        opponent(next).forEachIndexed { index, (oi, oj) ->
            next[index][oi, oj] = if (turn == Piece.WHITE) Piece.BLACK else Piece.WHITE
            if (next[index].winner != null) {
                val (i, j) = coordinates[index]
                val data = BufferData.Finish(
                    current = current[index],
                    coordinate = i to j,
                    isWinner = false,
                )
                bufferData.add(data)
                removed.add(index)
            }
        }
        removed.reversed().forEach {
            coordinates.removeAt(it)
            current.removeAt(it)
            next.removeAt(it)
        }
        removed.clear()

        repeat(HANDS) {
            next
                .select(runBlocking { network.expect(next) })
                .forEachIndexed { index, (ni, nj) ->
                    next[index][ni, nj] = turn
                    if (next[index].winner != null) {
                        val (i, j) = coordinates[index]
                        val data = BufferData.Finish(
                            current = current[index],
                            coordinate = i to j,
                            isWinner = true,
                        )
                        bufferData.add(data)
                        removed.add(index)
                    }
                }
            removed.reversed().forEach {
                coordinates.removeAt(it)
                current.removeAt(it)
                next.removeAt(it)
            }
            removed.clear()

            opponent(next).forEachIndexed { index, (oi, oj) ->
                next[index][oi, oj] = if (turn == Piece.WHITE) Piece.BLACK else Piece.WHITE
                if (next[index].winner != null) {
                    val (i, j) = coordinates[index]
                    val data = BufferData.Finish(
                        current = current[index],
                        coordinate = i to j,
                        isWinner = false,
                    )
                    bufferData.add(data)
                    removed.add(index)
                }
            }
            removed.reversed().forEach {
                coordinates.removeAt(it)
                current.removeAt(it)
                next.removeAt(it)
            }
            removed.clear()
        }

        repeat(next.size) {
            val data = BufferData.Continue(
                current = current[it],
                coordinate = coordinates[it],
                next = next[it],
            )
            bufferData.add(data)
        }
        return bufferData
    }
}

private sealed interface BufferData {
    val current: Board
    val coordinate: Pair<Int, Int>

    data class Finish(
        override val current: Board,
        override val coordinate: Pair<Int, Int>,
        val isWinner: Boolean,
    ) : BufferData

    data class Continue(
        override val current: Board,
        override val coordinate: Pair<Int, Int>,
        val next: Board,
    ) : BufferData
}

private fun List<Board>.selectRandom(seed: Int? = null): List<Pair<Int, Int>> {
    check(all { it.winner == null })
    val random = seed?.let { Random(it) } ?: Random
    return map { board ->
        val placeable = (0 until board.col).flatMap { i ->
            (0 until board.row)
                .filter { j -> board[i, j] == null }
                .map { j -> i to j }
        }
        placeable.random(random)
    }
}

// 候補手から実際に打てる手を選ぶ
private fun List<Board>.select(candidate: Batch<IOType.D1>): List<Pair<Int, Int>> =
    mapIndexed { index, board ->
        val expect = candidate[index]
        repeat(board.col * board.row) {
            val maxIndex = expect.maxIndex().unwrap().roundToInt()
            val (i, j) = maxIndex / board.row to maxIndex % board.row
            if (board[i, j] == null) return@mapIndexed i to j
            expect[i * board.row + j] = -Float.MAX_VALUE
        }
    error("invalid board. $this")
}
