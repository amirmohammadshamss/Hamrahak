package ir.rosheno.launcher.presentation.contacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.model.PhoneContact
import ir.rosheno.launcher.domain.phone.comparableSmsAddress
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.repository.PhoneContactsRepository
import ir.rosheno.launcher.platform.normalizeDialNumber
import ir.rosheno.launcher.platform.usesEmergencyDialer
import ir.rosheno.launcher.presentation.common.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactsUiState(
    val contacts: List<PhoneContact> = emptyList(),
    val allowed: Boolean = false,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val query: String = "",
    val adding: Boolean = false,
    val draftName: String = "",
    val draftNumber: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val saveNotice: UiText.Resource? = null,
    val error: UiText? = null,
    val selectedId: Long? = null,
    val calling: String? = null,
    val waiting: String? = null,
    val callDenied: Boolean = false,
) {
    val canSave get() = !saving && draftName.isNotBlank() && normalizePhone(draftNumber) != null
    val filtered: List<PhoneContact> get() {
        val search = query.trim()
        return contacts.filter { it.name.contains(search, ignoreCase = true) ||
            (search.isNotBlank() && it.numbers.any { number ->
                comparableSmsAddress(number).contains(comparableSmsAddress(search))
            }) }
    }
}

class ContactsViewModel(
    private val savedState: SavedStateHandle,
    private val repository: PhoneContactsRepository,
    private val io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutable = MutableStateFlow(ContactsUiState(
        query = savedState["query"] ?: "", adding = savedState["adding"] ?: false,
        draftName = savedState["name"] ?: "", draftNumber = savedState["number"] ?: "",
        selectedId = savedState["selected"], calling = savedState["calling"],
        waiting = savedState["waiting"], callDenied = savedState["denied"] ?: false,
        saveNotice = savedState.get<Int>("notice")?.let { UiText.Resource(it) },
    ))
    val state = mutable.asStateFlow()
    private var readJob: Job? = null
    private var releaseObserver: (() -> Unit)? = null
    private var active = false

    private fun change(transform: (ContactsUiState) -> ContactsUiState) {
        val next = transform(mutable.value)
        mutable.value = next
        savedState["query"] = next.query; savedState["adding"] = next.adding
        savedState["name"] = next.draftName; savedState["number"] = next.draftNumber
        savedState["selected"] = next.selectedId; savedState["calling"] = next.calling
        savedState["waiting"] = next.waiting; savedState["denied"] = next.callDenied
        savedState["notice"] = next.saveNotice?.id
    }

    fun start(allowed: Boolean) {
        active = true
        if (!allowed) { releaseObserver?.invoke(); releaseObserver = null }
        if (allowed && releaseObserver == null) {
            try { releaseObserver = repository.observe { if (active) refresh(state.value.allowed) } }
            catch (_: SecurityException) { refresh(false); return }
        }
        refresh(allowed)
    }

    fun stop() {
        active = false
        releaseObserver?.invoke(); releaseObserver = null
        readJob?.cancel()
    }

    fun refresh(allowed: Boolean = state.value.allowed) {
        readJob?.cancel()
        if (!allowed) {
            change { it.copy(allowed = false, contacts = emptyList(), selectedId = null, loading = false, failed = false) }
            return
        }
        readJob = viewModelScope.launch {
            change { it.copy(allowed = true, loading = true, failed = false) }
            try {
                val contacts = withContext(io) { repository.load() }
                change { it.copy(contacts = contacts, loading = false) }
            } catch (error: CancellationException) { throw error }
            catch (_: SecurityException) {
                releaseObserver?.invoke(); releaseObserver = null
                change { it.copy(allowed = false, contacts = emptyList(), selectedId = null, loading = false) }
            } catch (_: Exception) { change { it.copy(failed = true, contacts = emptyList(), loading = false) } }
        }
    }

    fun search(query: String) = change { it.copy(query = query) }
    fun editName(name: String) = change { it.copy(draftName = name.take(100)) }
    fun editNumber(number: String) = change { it.copy(draftNumber = number.take(32)) }
    fun add() = change { it.copy(adding = true, saved = false, error = null, saveNotice = null) }
    fun cancelAdd() { if (!state.value.saving) change { it.copy(adding = false, saved = false, error = null, saveNotice = null) } }
    fun select(id: Long?) = change { it.copy(selectedId = id) }
    fun dismissCall() = change { it.copy(calling = null) }
    fun consumeCall(): String? = state.value.calling.also { dismissCall() }

    /** True requests Android permission; a grant still requires explicit confirmation. */
    fun requestCall(number: String, canCall: Boolean): Boolean {
        val safe = normalizeDialNumber(number) ?: return false
        val needsPermission = !usesEmergencyDialer(safe) && !canCall
        change { it.copy(selectedId = null, callDenied = false,
            waiting = if (needsPermission) safe else null, calling = if (needsPermission) null else safe) }
        return needsPermission
    }
    fun callPermissionResult(granted: Boolean) = change { it.copy(waiting = null,
        calling = if (granted) it.waiting else null, callDenied = !granted) }
    fun writePermissionResult(granted: Boolean) = change { it.copy(saveNotice = if (granted)
        UiText.Resource(R.string.contact_write_permission_granted) else UiText.Resource(R.string.contact_write_permission_denied)) }

    fun save(name: String = state.value.draftName, number: String = state.value.draftNumber) {
        if (state.value.saving || state.value.saved || name.isBlank() || normalizePhone(number) == null) return
        change { it.copy(saving = true, error = null, saveNotice = null) }
        viewModelScope.launch {
            try {
                withContext(io) { repository.create(name, number) }
                change { it.copy(saving = false, saved = true, adding = false, draftName = "", draftNumber = "",
                    saveNotice = UiText.Resource(R.string.contact_saved)) }
                if (active) refresh()
            } catch (error: CancellationException) { throw error }
            catch (_: SecurityException) { change { it.copy(saving = false,
                error = UiText.Resource(R.string.error_contact_write_permission)) } }
            catch (_: Exception) { change { it.copy(saving = false,
                error = UiText.Resource(R.string.error_contact_save)) } }
        }
    }
    override fun onCleared() { stop() }
}
