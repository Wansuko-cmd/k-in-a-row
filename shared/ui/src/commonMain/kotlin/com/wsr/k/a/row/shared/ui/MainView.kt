package com.wsr.k.a.row.shared.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wsr.k.a.row.shared.model.helloWorld
import com.wsr.k.a.row.shared.ui.theme.KInARowTheme

@Composable
fun MainView() {
    KInARowTheme {
        Text(text = helloWorld())
    }
}
