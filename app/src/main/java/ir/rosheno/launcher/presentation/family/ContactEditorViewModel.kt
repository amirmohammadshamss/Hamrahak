package ir.rosheno.launcher.presentation.family

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository
import ir.rosheno.launcher.presentation.common.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactEditorUiState(val name: String = "", val number: String = "", val photo: String? = null,
    val busy: Boolean = false, val error: UiText? = null) {
    val validNumber get() = normalizePhone(number) != null
    val canSave get() = !busy && name.isNotBlank() && validNumber
}

class ContactEditorViewModel(
    private val saved: SavedStateHandle,
    private val photos: ContactPhotoRepository,
    private val io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutable = MutableStateFlow(ContactEditorUiState(
        name = saved["name"] ?: "", number = saved["number"] ?: "", photo = saved["photo"],
    ))
    val state = mutable.asStateFlow()
    private var stageJob: Job? = null
    private fun change(transform: (ContactEditorUiState) -> ContactEditorUiState) {
        val next = transform(mutable.value)
        mutable.value = next
        saved["name"] = next.name; saved["number"] = next.number; saved["photo"] = next.photo
    }
    fun begin(session: String, initial: FamilyContact?) {
        if (saved.get<String>("session") == session) return
        stageJob?.cancel()
        discard(state.value.photo)
        saved["session"] = session
        change { ContactEditorUiState(initial?.name ?: "", initial?.number ?: "", initial?.photo) }
    }
    fun name(value: String) { if (!state.value.busy) change { it.copy(name = value.take(80)) } }
    fun number(value: String) { if (!state.value.busy) change { it.copy(number = value.take(32)) } }
    fun stagePhoto(source: String) {
        if (state.value.busy) return
        change { it.copy(busy = true, error = null) }
        stageJob = viewModelScope.launch {
            var stagedPath: String? = null
            var published = false
            try {
                val path = withContext(io) { photos.stage(source).also { stagedPath = it } }
                val previous = state.value.photo
                change { it.copy(photo = path, busy = false) }
                published = true
                withContext(io) { runCatching { photos.discardDraft(previous) } }
            } catch (error: CancellationException) {
                // withContext can be cancelled after staging but before returning to Main.
                if (!published) withContext(io + NonCancellable) { runCatching { photos.discardDraft(stagedPath) } }
                throw error
            } catch (_: Exception) { change { it.copy(busy = false,
                error = UiText.Resource(R.string.error_photo_stage)) } }
        }
    }

    fun removePhoto() {
        if (state.value.busy) return
        val previous = state.value.photo
        change { it.copy(photo = null) }
        discard(previous)
    }
    fun cancel(): Boolean {
        if (state.value.busy) return false
        discard(state.value.photo)
        return true
    }
    private fun discard(path: String?) {
        viewModelScope.launch(io + NonCancellable) { runCatching { photos.discardDraft(path) } }
    }
}
