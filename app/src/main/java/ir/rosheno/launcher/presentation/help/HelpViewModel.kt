package ir.rosheno.launcher.presentation.help

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HelpUiState(val selected: Int = -1, val index: Int = 0)
class HelpViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(HelpUiState(saved["selected"] ?: -1, saved["index"] ?: 0))
    val state = mutable.asStateFlow()
    private fun change(next: HelpUiState) { mutable.value = next; saved["selected"] = next.selected; saved["index"] = next.index }
    fun select(topic: Int) = change(HelpUiState(topic, 0))
    fun topics() = change(state.value.copy(selected = -1))
    fun previous() { if (state.value.index > 0) change(state.value.copy(index = state.value.index - 1)) else topics() }
    fun next(lastIndex: Int) { if (state.value.index < lastIndex) change(state.value.copy(index = state.value.index + 1)) else topics() }
}
