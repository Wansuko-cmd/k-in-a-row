package com.wsr.k.a.row.shared.ui

class MainPresenter : Presenter<MainUiState, UiEvent>(MainUiState())

data class MainUiState(val value: String = "uiState") : UiState
