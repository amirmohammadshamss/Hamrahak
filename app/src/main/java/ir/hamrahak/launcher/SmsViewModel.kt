package ir.hamrahak.launcher

import android.Manifest
import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SmsUiState(val messages: List<TextMessage> = emptyList(), val loading: Boolean = false,
    val canRead: Boolean = false, val hasMore: Boolean = false, val error: String? = null,
    val recipient: String = "", val draft: String = "", val submitting: Boolean = false,
    val report: SmsSendReport? = null, val reportExpired: Boolean = false)

class SmsViewModel @JvmOverloads constructor(application: Application, private val saved: SavedStateHandle,
    private val repository: SmsRepository = SmsRepository(application)) : AndroidViewModel(application) {
    private val mutable = MutableStateFlow(SmsUiState(recipient = saved["recipient"] ?: "", draft = saved["draft"] ?: ""))
    val state = mutable.asStateFlow()
    private var limit = 200
    private var visible = false
    private var observing = false
    private var readJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { if (visible) refresh() }
    }
    private val resultListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshReport() }
    private var timeout: Job? = null
    private var reportRead: Job? = null
    init { repository.journal.preferences.registerOnSharedPreferenceChangeListener(resultListener); refreshReport() }

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
            val result = withContext(Dispatchers.IO) { runCatching { repository.source.load(limit) } }
            mutable.value = result.fold(
                { mutable.value.copy(messages = it.messages, hasMore = it.hasMore, loading = false) },
                { mutable.value.copy(messages = emptyList(), loading = false,
                    error = "خواندن پیامک‌ها در این نصب مجاز نیست یا در دسترس نیست. می‌توانید پیامک‌های گوشی را باز کنید.") })
        }
    }
    fun more() { limit = (limit + 200).coerceAtMost(5000); refresh() }
    fun draft(recipient: String = mutable.value.recipient, text: String = mutable.value.draft) {
        saved["recipient"] = recipient.take(32); saved["draft"] = text.take(5000)
        mutable.value = mutable.value.copy(recipient = recipient.take(32), draft = text.take(5000))
    }
    fun sendConfirmed(address: String, body: String) {
        if (mutable.value.submitting || mutable.value.report?.let { !it.complete && !mutable.value.reportExpired } == true) return
        if (!hasPermission(Manifest.permission.SEND_SMS)) { error("برای فرستادن پیامک، اجازهٔ ارسال لازم است."); return }
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
                    "در تنظیمات گوشی، سیم‌کارت پیش‌فرض پیامک را انتخاب کنید و دوباره تلاش کنید."
                else "پیامک ارسال نشد. مجوز ارسال، سیم‌کارت و آنتن گوشی را بررسی کنید. متن شما در پیش‌نویس مانده است."
                mutable.value = mutable.value.copy(submitting = false, error = detail)
            }
        }
    }
    fun error(text: String?) { mutable.value = mutable.value.copy(error = text) }
    private fun refreshReport() {
        reportRead?.cancel()
        reportRead = viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { repository.journal.read() }
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
    override fun onCleared() {
        stop(); repository.journal.preferences.unregisterOnSharedPreferenceChangeListener(resultListener)
    }
}
