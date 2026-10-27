package ir.rosheno.launcher.presentation.sms

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ir.rosheno.launcher.R
import ir.rosheno.launcher.di.AppContainer
import ir.rosheno.launcher.domain.phone.comparableSmsAddress
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.repository.MessageRepository
import ir.rosheno.launcher.presentation.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsViewModel @JvmOverloads constructor(application: Application, private val saved: SavedStateHandle,
    private val repository: MessageRepository = AppContainer(application).messages) : AndroidViewModel(application) {
    private val mutable = MutableStateFlow(SmsUiState(recipient = saved["recipient"] ?: "", draft = saved["draft"] ?: "",
        mode = saved["mode"] ?: "inbox", composeOrigin = saved["composeOrigin"] ?: "inbox", threadId = saved["threadId"],
        confirmationAddress = saved["confirmationAddress"], confirmationBody = saved["confirmationBody"] ?: "",
        choosingContact = saved["choosingContact"] ?: false, replaceDraftFor = saved["replaceDraftFor"],
        clearingDraft = saved["clearingDraft"] ?: false))
    val state = mutable.asStateFlow()
    private var limit = 200
    private var visible = false
    private var observing = false
    private var readJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { if (visible) refresh() }
    }
    private var timeout: Job? = null
    private var reportRead: Job? = null
    private val releaseReportObserver = repository.observeReports { refreshReport() }
    init { refreshReport() }

    fun hasPermission(permission: String): Boolean = ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED
    fun start() {
        visible = true
        if (!observing && hasPermission(Manifest.permission.READ_SMS)) observing = runCatching {
            getApplication<Application>().contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
        }.isSuccess
        refresh()
    }
    fun stop() {
        if (observing) runCatching { getApplication<Application>().contentResolver.unregisterContentObserver(observer) }
        observing = false
        visible = false
        readJob?.cancel()
    }
    fun refresh() {
        val allowed = hasPermission(Manifest.permission.READ_SMS)
        if (!allowed) { mutable.value = mutable.value.copy(canRead = false, loading = false, messages = emptyList()); return }
        readJob?.cancel()
        readJob = viewModelScope.launch {
            mutable.value = mutable.value.copy(canRead = true, loading = true, error = null)
            val result = withContext(Dispatchers.IO) { runCatching { repository.load(limit) } }
            mutable.value = result.fold(
                { mutable.value.copy(messages = it.messages, hasMore = it.hasMore, loading = false) },
                { mutable.value.copy(messages = emptyList(), loading = false,
                    error = UiText.Resource(R.string.error_sms_read)) })
        }
    }
    fun more() { limit = (limit + 200).coerceAtMost(5000); refresh() }
    fun draft(recipient: String = mutable.value.recipient, text: String = mutable.value.draft) {
        saved["recipient"] = recipient.take(32); saved["draft"] = text.take(5000)
        mutable.value = mutable.value.copy(recipient = recipient.take(32), draft = text.take(5000))
    }
    fun sendConfirmed(address: String, body: String) {
        if (mutable.value.submitting || mutable.value.report?.let { !it.complete && !mutable.value.reportExpired } == true) return
        if (!hasPermission(Manifest.permission.SEND_SMS)) { error(UiText.Resource(R.string.error_sms_send_permission)); return }
        mutable.value = mutable.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { repository.send(address, body) } }
            if (result.isSuccess) {
                saved["sendingToken"] = result.getOrThrow()
                saved["sendingAddress"] = address
                saved["sendingBody"] = body
                refreshReport()
            } else {
                val detail = if (result.exceptionOrNull()?.message == "NO_DEFAULT_SIM")
                    UiText.Resource(R.string.error_sms_default_sim)
                else UiText.Resource(R.string.error_sms_send)
                mutable.value = mutable.value.copy(submitting = false, error = detail)
            }
        }
    }
    fun error(text: String?) { mutable.value = mutable.value.copy(error = text?.let { UiText.Dynamic(it) }) }
    fun error(text: UiText) { mutable.value = mutable.value.copy(error = text) }
    private fun refreshReport() {
        reportRead?.cancel()
        reportRead = viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { repository.readReport() }
            val currentSubmission = report != null && report.token == saved.get<String>("sendingToken")
            // Submission is not delivery: retain failed/unknown drafts and prevent accidental resends.
            if (report?.sent == true && report.token == saved.get<String>("sendingToken")) {
                if (mutable.value.recipient == saved.get<String>("sendingAddress") && mutable.value.draft == saved.get<String>("sendingBody")) draft(text = "")
                saved["sendingToken"] = null
                saved["sendingAddress"] = null
                saved["sendingBody"] = null
            }
            mutable.value = mutable.value.copy(report = report, reportExpired = report?.expired == true,
                submitting = if (currentSubmission) false else mutable.value.submitting)
            timeout?.cancel()
            if (report != null && !report.complete && !report.expired) timeout = viewModelScope.launch {
                delay((report.created + 300_000 - System.currentTimeMillis()).coerceAtLeast(1)); refreshReport()
            }
        }
    }
    private fun screenChange(transform: (SmsUiState) -> SmsUiState) {
        val next = transform(mutable.value)
        mutable.value = next
        saved["mode"] = next.mode; saved["composeOrigin"] = next.composeOrigin; saved["threadId"] = next.threadId
        saved["confirmationAddress"] = next.confirmationAddress; saved["confirmationBody"] = next.confirmationBody
        saved["choosingContact"] = next.choosingContact; saved["replaceDraftFor"] = next.replaceDraftFor
        saved["clearingDraft"] = next.clearingDraft
    }
    fun inbox() = screenChange { it.copy(mode = "inbox") }
    fun openThread(id: Long) = screenChange { it.copy(threadId = id, mode = "thread") }
    fun back() = screenChange { it.copy(mode = if (it.mode == "compose") it.composeOrigin else "inbox") }
    fun compose(address: String?) {
        val current = state.value
        val replace = address != null && current.draft.isNotBlank() &&
            comparableSmsAddress(address) != comparableSmsAddress(current.recipient)
        if (!replace && address != null) draft(recipient = address)
        screenChange { it.copy(composeOrigin = current.mode, replaceDraftFor = if (replace) address else null,
            mode = if (replace) it.mode else "compose") }
    }
    fun confirmDraft() {
        val current = state.value
        if (normalizePhone(current.recipient) == null || current.draft.isBlank()) {
            error(UiText.Resource(R.string.error_sms_draft_required))
        } else screenChange { it.copy(confirmationAddress = current.recipient, confirmationBody = current.draft) }
    }
    fun dismissConfirmation() = screenChange { it.copy(confirmationAddress = null) }
    fun sendConfirmation() {
        val address = state.value.confirmationAddress ?: return
        val body = state.value.confirmationBody
        dismissConfirmation()
        sendConfirmed(address, body)
    }
    fun chooseContact(show: Boolean) = screenChange { it.copy(choosingContact = show) }
    fun selectContact(number: String) { draft(recipient = number); chooseContact(false) }
    fun dismissReplaceDraft() = screenChange { it.copy(replaceDraftFor = null) }
    fun replaceDraft(replace: Boolean) {
        val address = state.value.replaceDraftFor ?: return
        if (replace) draft(recipient = address, text = "")
        screenChange { it.copy(replaceDraftFor = null, mode = "compose") }
    }
    fun clearDraft(show: Boolean) = screenChange { it.copy(clearingDraft = show) }
    fun confirmClearDraft() { draft(text = "", recipient = ""); clearDraft(false) }
    override fun onCleared() {
        stop(); releaseReportObserver()
    }
}
