package com.wsr.k.a.row.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.takahirom.rin.RetainedObserver
import io.github.takahirom.rin.rememberRetained
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface UiState
interface UiEvent

abstract class Presenter<State : UiState, Event : UiEvent>(initialValue: State) {
    protected val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate,
    )

    var uiState by mutableStateOf(initialValue)
        protected set

    private val _uiEvent = MutableSharedFlow<Event>()
    val uiEvent = _uiEvent.asSharedFlow()

    suspend fun emitEvent(event: Event) = _uiEvent.emit(event)

    open fun onRemembered() {}

    open fun onForgotten() {
        scope.cancel()
    }
}

@Composable
fun <T : Presenter<*, *>> rememberPresenter(factory: () -> T): T {
    val presenter = rememberRetained { factory() }
    rememberRetained(presenter.hashCode().toString()) {
        object : RetainedObserver {
            override fun onRemembered() {
                presenter.onRemembered()
            }

            override fun onForgotten() {
                presenter.onForgotten()
            }
        }
    }
    return presenter
}
