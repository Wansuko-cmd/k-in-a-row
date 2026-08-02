@file:Suppress("NonAsciiCharacters")

package com.wsr.k.a.row.shared.model

import com.wsr.k.a.row.shared.domain.Board
import com.wsr.k.a.row.shared.domain.Piece
import com.wsr.k.a.row.shared.model.dqn.DQNInputConverter
import com.wsr.knist.batch.Batch
import com.wsr.knist.batch.get
import com.wsr.knist.core.IOType
import com.wsr.knist.core.d1
import com.wsr.knist.core.reduction.maxIndex
import com.wsr.knist.core.set
import com.wsr.knist.core.unwrap
import com.wsr.knist.network.Network
import com.wsr.knist.network.NetworkSerializer
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test

private const val COL = 8
private const val ROW = 8
private const val K = 4

private val dqn: Network.Src1.Sink1<List<Board>, Batch<IOType.D1>> = run {
    NetworkSerializer.register(DQNInputConverter::class)
    FileSystem.SYSTEM
        .source("dqn.cbor".toPath())
        .buffer()
        .use { it.readByteArray() }
        .let { bytes -> Network.Src1.Sink1.fromCbor(bytes) }
}

class ModelVSTest {
    val target: (board: Board, turn: Piece) -> Pair<Int, Int> = { board , turn -> board.selectDqn(turn) }
    val opposite: (board: Board, turn: Piece) -> Pair<Int, Int> = { board , _ -> board.selectRandom()!! }

    @Test
    fun `target vs opposite`() {
        var targetWin = 0
        var oppositeWin = 0
        repeat(50) {
            val b1 = Board(col = COL, row = ROW, k = K)
            while (b1.winner == null) {
                val (bi, bj) = target(b1, Piece.BLACK)
                b1[bi, bj] = Piece.BLACK
                if (b1.winner != null) {
                    targetWin++
                    break
                }

                val (wi, wj) = opposite(b1, Piece.WHITE)
                b1[wi, wj] = Piece.WHITE
                if (b1.winner != null) {
                    oppositeWin++
                    break
                }
            }

            val b2 = Board(col = COL, row = ROW, k = K)
            while (b2.winner == null) {
                val (bi, bj) = opposite(b2, Piece.BLACK)
                b2[bi, bj] = Piece.BLACK
                if (b2.winner != null) {
                    oppositeWin++
                    break
                }

                val (wi, wj) = target(b2, Piece.WHITE)
                b2[wi, wj] = Piece.WHITE
                if (b2.winner != null) {
                    targetWin++
                    break
                }
            }
        }
        println("target: $targetWin, opposite: $oppositeWin")
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

    private fun Board.selectDqn(turn: Piece): Pair<Int, Int> {
        val model = dqn.replaceSource(
            converter = DQNInputConverter(
                col = COL,
                row = ROW,
                turn = turn,
            ),
        )
        val expect = runBlocking { model.expect(listOf(this@selectDqn))[0] }
        return this.select(expect)
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
}
