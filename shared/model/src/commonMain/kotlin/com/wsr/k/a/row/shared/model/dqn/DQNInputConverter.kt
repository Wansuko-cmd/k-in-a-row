package com.wsr.k.a.row.shared.model.dqn

import com.wsr.k.a.row.shared.domain.Board
import com.wsr.k.a.row.shared.domain.Piece
import com.wsr.knist.base.data.DataBuffer
import com.wsr.knist.batch.Batch
import com.wsr.knist.batch.d3
import com.wsr.knist.core.IOType
import com.wsr.knist.network.converter.Converter
import kotlinx.serialization.Serializable

@Serializable
data class DQNInputConverter(
    val col: Int,
    val row: Int,
    val turn: Piece,
) : Converter.D3<List<Board>>() {
    // ch1: 自分の駒 or それ以外
    // ch2: 相手の駒 or それ以外
    override val outputI: Int = 2
    override val outputJ: Int = col
    override val outputK: Int = row

    override fun encode(input: List<Board>): Batch<IOType.D3> {
        val value = FloatArray(input.size * outputI * outputJ * outputK)
        input.forEachIndexed { index, board ->
            repeat(col) { i ->
                repeat(row) { j ->
                    val offset = when (board[i, j]) {
                        null -> return@repeat
                        turn -> 2 * index
                        else -> 2 * index + 1
                    }
                    value[(offset * col + i) * row + j] = 1f
                }
            }
        }
        return Batch.d3(
            size = input.size,
            i = 2, j = col, k = row,
            value = DataBuffer.create(value),
        )
    }

    override fun decode(input: Batch<IOType.D3>): List<Board> {
        error("ここまでは到達しない")
    }
}
