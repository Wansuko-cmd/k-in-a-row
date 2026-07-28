package com.wsr.k.a.row.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wsr.k.a.row.shared.domain.Piece
import com.wsr.k.a.row.shared.ui.theme.KInARowTheme

@Composable
fun MainView() {
    KInARowTheme {
        val presenter = rememberPresenter { MainPresenter() }
        val uiState = presenter.uiState
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Board(
                board = uiState.board,
                onClick = { i, j -> presenter.onClick(i, j, uiState.turn) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                modifier = Modifier.height(24.dp),
                text = when (uiState.winner) {
                    null -> ""
                    Piece.BLACK -> "黒の勝ち"
                    Piece.WHITE -> "白の勝ち"
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { presenter.onReset() },
            ) {
                Text(text = "リセット", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun Board(
    board: BoardUiState,
    onClick: (i: Int, j: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        maxItemsInEachRow = board.row,
        modifier = modifier.border(width = 2.dp, color = Color.Gray),
    ) {
        repeat(board.col) { i ->
            repeat(board.row) { j ->
                Piece(
                    value = board[i, j],
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .border(width = 1.dp, color = Color.LightGray)
                        .clickable { onClick(i, j) },
                )
            }
        }
    }
}

@Composable
private fun Piece(value: Piece?, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        if (value != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .background(
                        color = when (value) {
                            Piece.WHITE -> Color.White
                            Piece.BLACK -> Color.Black
                        },
                        shape = CircleShape,
                    )
                    .border(width = 1.dp, color = Color.Gray, shape = CircleShape),
            )
        }
    }
}
