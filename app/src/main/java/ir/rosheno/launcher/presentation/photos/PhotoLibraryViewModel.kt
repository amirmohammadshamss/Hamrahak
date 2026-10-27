package ir.rosheno.launcher.presentation.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.repository.CaptureRepository
import ir.rosheno.launcher.presentation.common.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoLibraryUiState(val photos: List<String> = emptyList(), val loading: Boolean = true, val error: UiText? = null)

class PhotoLibraryViewModel(
    private val captures: CaptureRepository,
    private val io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutable = MutableStateFlow(PhotoLibraryUiState())
    val state = mutable.asStateFlow()
    private var loadJob: Job? = null
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = true, error = null)
            try {
                val photos = withContext(io) { captures.list() }
                mutable.value = PhotoLibraryUiState(photos, loading = false)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutable.value = mutable.value.copy(loading = false,
                error = UiText.Resource(R.string.error_photo_library_load)) }
        }
    }
}
