package com.wsr.k.a.row.shared.model

import com.wsr.k.a.row.shared.domain.Board

interface Model {
    suspend fun choice(board: Board): Pair<Int, Int>?
}
