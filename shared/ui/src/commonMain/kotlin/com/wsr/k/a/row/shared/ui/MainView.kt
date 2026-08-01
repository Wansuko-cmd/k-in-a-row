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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wsr.k.a.row.shared.domain.Piece
import com.wsr.k.a.row.shared.ui.theme.KInARowTheme

@Composable
fun MainView() {
    KInARowTheme {
        val presenter = rememberPresenter { MainPresenter() }
        MainScreen(
            uiState = presenter.uiState,
            onClickBoard = presenter::onClick,
            onClickReset = presenter::onReset,
        )
    }
}

@Composable
private fun MainScreen(
    uiState: MainUiState,
    onClickBoard: (i: Int, j: Int, turn: Piece) -> Unit,
    onClickReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 8.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Board(
            board = uiState.board,
            onClick = { i, j -> onClickBoard(i, j, uiState.turn) },
            modifier = Modifier.fillMaxWidth(),
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
            onClick = onClickReset,
        ) {
            Text(text = "リセット", fontSize = 16.sp)
        }
    }

    if (uiState.isLoading) {
        LoadingIndicator(modifier = Modifier.fillMaxSize())
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

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreen() {
    KInARowTheme {
        val board = BoardUiState(
            col = 8,
            row = 8,
            value = List(8 * 8) { index ->
                when (index % 3) {
                    1 -> Piece.WHITE
                    2 -> Piece.BLACK
                    else -> null
                }
            }
        )
        MainScreen(
            uiState = MainUiState(board = board),
            onClickBoard = { _, _, _ -> },
            onClickReset = {},
        )
    }
}
