package ir.rosheno.launcher.presentation.launcher

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import ir.rosheno.launcher.domain.model.LauncherConfig
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LauncherNavigationState(
    val screen: String = "home",
    val history: List<String> = emptyList(),
    val adding: Boolean = false,
    val editingId: String? = null,
    val editorSession: String = "",
    val callingId: String? = null,
    val reset: Boolean = false,
    val viewingPhoto: String? = null,
    val dialedNumber: String = "",
    val pendingHelpId: String? = null,
)

class LauncherNavigationViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(LauncherNavigationState(
        screen = saved["screen"] ?: "home", history = saved.get<ArrayList<String>>("history") ?: emptyList(),
        adding = saved["adding"] ?: false, editingId = saved["editingId"], editorSession = saved["editorSession"] ?: "",
        callingId = saved["callingId"], reset = saved["reset"] ?: false, viewingPhoto = saved["viewingPhoto"],
        dialedNumber = saved["dialedNumber"] ?: "", pendingHelpId = saved["pendingHelpId"],
    ))
    val state = mutable.asStateFlow()
    private fun change(transform: (LauncherNavigationState) -> LauncherNavigationState) {
        val next = transform(mutable.value)
        mutable.value = next
        saved["screen"] = next.screen; saved["history"] = ArrayList(next.history)
        saved["adding"] = next.adding; saved["editingId"] = next.editingId; saved["editorSession"] = next.editorSession
        saved["callingId"] = next.callingId; saved["reset"] = next.reset; saved["viewingPhoto"] = next.viewingPhoto
        saved["dialedNumber"] = next.dialedNumber; saved["pendingHelpId"] = next.pendingHelpId
    }
    fun navigate(destination: String) = change {
        it.copy(screen = destination, history = when {
            destination == "home" -> emptyList()
            destination == it.screen -> it.history
            else -> it.history + it.screen
        }, pendingHelpId = if (destination == "help-contact") it.pendingHelpId else null)
    }
    fun back() = change { it.copy(screen = it.history.lastOrNull() ?: "home", history = it.history.dropLast(1), pendingHelpId = null) }
    fun editContact(id: String? = null) = change {
        it.copy(adding = true, editingId = id, editorSession = UUID.randomUUID().toString())
    }
    fun dismissEditor() = change { it.copy(adding = false, editingId = null) }
    fun callContact(id: String?) = change { it.copy(callingId = id) }
    fun confirmReset(show: Boolean) = change { it.copy(reset = show) }
    fun dialNumber(number: String) = change { it.copy(dialedNumber = number.take(32)) }
    fun selectHelper(id: String) = change { it.copy(pendingHelpId = id) }
    fun viewPhoto(path: String) { change { it.copy(viewingPhoto = path) }; navigate("photo-view") }
    fun photoCaptured(path: String) { if (state.value.screen == "camera") viewPhoto(path) }
    fun homeSignal(signal: Int) {
        val previous = saved.get<Int>("homeSignal")
        saved["homeSignal"] = signal
        if (previous != null && previous != signal) change {
            it.copy(screen = "home", history = emptyList(), adding = false, editingId = null,
                callingId = null, reset = false, pendingHelpId = null)
        }
    }
    fun configurationChanged(config: LauncherConfig, saveToken: String?) {
        if (saveToken != null && saveToken != saved.get<String>("saveToken")) {
            saved["saveToken"] = saveToken
            dismissEditor()
        }
        val pending = state.value.pendingHelpId
        if (pending != null && state.value.screen == "help-contact" && config.helpContactId == pending &&
            config.contacts.any { it.id == pending }) {
            navigate("home")
            callContact(pending)
        }
    }
}
