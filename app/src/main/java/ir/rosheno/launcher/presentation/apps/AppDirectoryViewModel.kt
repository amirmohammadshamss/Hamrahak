package ir.rosheno.launcher.presentation.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import ir.rosheno.launcher.domain.model.LaunchableApp

class AppDirectoryViewModel(private val saved: SavedStateHandle) : ViewModel() {
    val query = saved.getStateFlow("query", "")
    fun search(value: String) { saved["query"] = value }
    fun filter(apps: List<LaunchableApp>) = apps.filter { it.label.contains(query.value, ignoreCase = true) }
}
