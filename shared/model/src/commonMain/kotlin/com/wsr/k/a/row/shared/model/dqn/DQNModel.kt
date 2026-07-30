package com.wsr.k.a.row.shared.model.dqn

import com.wsr.k.a.row.shared.domain.Board
import com.wsr.k.a.row.shared.model.Model
import com.wsr.knist.batch.Batch
import com.wsr.knist.batch.get
import com.wsr.knist.core.IOType
import com.wsr.knist.core.d1
import com.wsr.knist.core.reduction.maxIndex
import com.wsr.knist.core.set
import com.wsr.knist.core.unwrap
import com.wsr.knist.network.Network
import com.wsr.knist.network.NetworkSerializer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.math.roundToInt

private val model = FileSystem.RESOURCES.source("dqn.cbor".toPath()).buffer().use { it.readByteArray() }

class DQNModel(bytes: ByteArray = model) : Model {
    private val network: Network.Src1.Sink1<List<Board>, Batch<IOType.D1>>

    init {
        NetworkSerializer.apply {
            register(DQNInputConverter::class)
        }
        network = Network.Src1.Sink1.fromCbor(bytes)
    }

    override suspend fun choice(board: Board): Pair<Int, Int>? {
        if (board.winner != null) return null
        val expect = network.expect(input = listOf(board))[0]
        return board.select(expect)
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
