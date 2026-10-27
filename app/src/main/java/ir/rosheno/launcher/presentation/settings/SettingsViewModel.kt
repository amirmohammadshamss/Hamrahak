package ir.rosheno.launcher.presentation.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class SettingsViewModel(private val saved: SavedStateHandle) : ViewModel() {
    val deletingId = saved.getStateFlow<String?>("deletingId", null)
    fun requestDelete(id: String) { saved["deletingId"] = id }
    fun dismissDelete() { saved["deletingId"] = null }
    fun consumeDelete(): String? = deletingId.value.also { dismissDelete() }
}
