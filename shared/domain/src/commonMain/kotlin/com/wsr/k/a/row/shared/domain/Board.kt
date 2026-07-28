package com.wsr.k.a.row.shared.domain

class Board(
    val col: Int,
    val row: Int,
    val k: Int,
    private val value: IntArray = IntArray(col * row),
) {
    init {
        check(k < col || k < row)
    }

    var winner: Piece? = null
        private set

    operator fun get(i: Int, j: Int): Piece? {
        val index = i * row + j
        check(i in 0 until col && j in 0 until row)
        return Piece.from(value[index])
    }

    operator fun set(i: Int, j: Int, piece: Piece) {
        if (winner != null) return

        val index = i * row + j
        check(Piece.from(value[index]) == null)
        value[index] = piece.value

        winner = scanWinner()
    }

    private fun scanWinner(): Piece? {
        for (i in 0 until col) {
            for (j in 0 until row) {
                val index = i * row + j
                val piece = Piece.from(value[index]) ?: continue
                if (moreK(i, j, piece)) return piece
            }
        }
        return null
    }

    private fun moreK(i: Int, j: Int, piece: Piece): Boolean {
        val max = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1, 0 to 1,
            1 to -1, 1 to 0, 1 to 1,
        )
            .maxOf { (strideI, strideJ) -> count(i, j, strideI, strideJ, piece) }
        return k <= max
    }

    private fun count(i: Int, j: Int, strideI: Int, strideJ: Int, piece: Piece): Int {
        var currentI = i
        var currentJ = j
        var count = 0
        while (currentI in 0 until col && currentJ in 0 until row) {
            val index = currentI * row + currentJ
            if (Piece.from(value[index]) != piece) return count
            currentI += strideI
            currentJ += strideJ
            count++
        }
        return count
    }
}

enum class Piece(val value: Int) {
    WHITE(1),
    BLACK(2);

    companion object {
        fun from(value: Int) = when (value) {
            1 -> WHITE
            2 -> BLACK
            else -> null
        }
    }
}
