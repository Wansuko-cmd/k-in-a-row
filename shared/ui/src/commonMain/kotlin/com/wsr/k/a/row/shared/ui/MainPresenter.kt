package com.wsr.k.a.row.shared.ui

import com.wsr.k.a.row.shared.domain.Board
import com.wsr.k.a.row.shared.domain.Piece
import com.wsr.k.a.row.shared.model.RandomModel
import kotlinx.coroutines.runBlocking

private const val COL = 8
private const val ROW = 8
private const val K = 4

class MainPresenter(
    private var board: Board = Board(col = COL, row = ROW, k = K),
) : Presenter<MainUiState, UiEvent>(MainUiState(board = BoardUiState.from(board))) {
    fun onClick(i: Int, j: Int, turn: Piece) {
        if (i !in 0 until COL || j !in 0 until ROW || board.winner != null) return
        board[i, j] = turn

        uiState = uiState.copy(
            board = BoardUiState.from(board),
            turn = when (turn) {
                Piece.BLACK -> Piece.WHITE
                Piece.WHITE -> Piece.BLACK
            },
            winner = board.winner,
        )

        placeCPU()
    }

    private fun placeCPU() {
        val model = when (uiState.cpu) {
            CPUUiState.Random -> RandomModel()
        }
        val (i, j) = runBlocking { model.choice(board) } ?: return
        board[i, j] = uiState.turn

        uiState = uiState.copy(
            board = BoardUiState.from(board),
            turn = when (uiState.turn) {
                Piece.BLACK -> Piece.WHITE
                Piece.WHITE -> Piece.BLACK
            },
            winner = board.winner,
        )
    }

    fun onReset() {
        board = Board(col = COL, row = ROW, k = K)
        uiState = MainUiState(board = BoardUiState.from(board))
    }
}

data class MainUiState(
    val board: BoardUiState = BoardUiState(),
    val turn: Piece = Piece.BLACK,
    val winner: Piece? = null,
    val cpu: CPUUiState = CPUUiState.Random,
    val isLoading: Boolean = turn == Piece.WHITE,
) : UiState

data class BoardUiState(
    val col: Int = COL,
    val row: Int = ROW,
    val value: List<Piece?> = List(col * row) { null },
) {
    operator fun get(i: Int, j: Int) = value[i * row + j]

    companion object {
        fun from(board: Board) = BoardUiState(
            col = board.col,
            row = board.row,
            value = (0 until board.col).flatMap { i ->
                (0 until board.row).map { j ->
                    board[i, j]
                }
            }
        )
    }
}

enum class CPUUiState {
    Random;
}
