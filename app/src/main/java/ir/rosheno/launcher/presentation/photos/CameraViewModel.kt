package ir.rosheno.launcher.presentation.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.repository.CaptureRepository
import ir.rosheno.launcher.presentation.common.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CameraUiState(val saving: Boolean = false, val preparedPath: String? = null, val error: UiText? = null)

class CameraViewModel(
    private val captures: CaptureRepository,
    private val io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutable = MutableStateFlow(CameraUiState())
    val state = mutable.asStateFlow()
    private var preparation: Job? = null
    private var dispatchedPath: String? = null
    fun ready() { mutable.value = mutable.value.copy(error = null) }
    fun unavailable() { mutable.value = mutable.value.copy(error = UiText.Resource(R.string.error_camera_unavailable)) }
    fun requestCapture() {
        if (state.value.saving) return
        mutable.value = CameraUiState(saving = true)
        preparation = viewModelScope.launch {
            try {
                val path = withContext(io) { captures.create() }
                mutable.value = mutable.value.copy(preparedPath = path)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutable.value = CameraUiState(error = UiText.Resource(R.string.error_capture_save)) }
        }
    }
    /** Consumed before CameraX starts, so recreation cannot dispatch the same capture twice. */
    fun takePreparedCapture(): String? {
        val path = state.value.preparedPath ?: return null
        dispatchedPath = path
        mutable.value = mutable.value.copy(preparedPath = null)
        return path
    }
    fun captureSaved(path: String): Boolean {
        if (dispatchedPath != path) return false
        dispatchedPath = null
        mutable.value = CameraUiState()
        return true
    }
    fun captureFailed(path: String) {
        viewModelScope.launch(io + NonCancellable) { runCatching { captures.delete(path) } }
        if (dispatchedPath == path) {
            dispatchedPath = null
            mutable.value = CameraUiState(error = UiText.Resource(R.string.error_capture_save))
        }
    }
    fun cancelPreparation() {
        if (dispatchedPath != null) return
        preparation?.cancel()
        state.value.preparedPath?.let { path -> viewModelScope.launch(io + NonCancellable) { runCatching { captures.delete(path) } } }
        mutable.value = mutable.value.copy(saving = false, preparedPath = null)
    }
}
