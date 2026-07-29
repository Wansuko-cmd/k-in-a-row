package com.wsr.k.a.row.shared.model

import com.wsr.k.a.row.shared.domain.Board

class RandomModel : Model {
    override suspend fun choice(board: Board): Pair<Int, Int>? {
        if (board.winner != null) return null
        val placeable = (0 until board.col).flatMap { i ->
            (0 until board.row)
                .filter { j -> board[i, j] == null }
                .map { j -> i to j }
        }
        return placeable.randomOrNull()
    }
}
