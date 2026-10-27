package ir.rosheno.launcher.data.sms

import android.content.Context
import ir.rosheno.launcher.domain.model.MessagePage
import ir.rosheno.launcher.domain.model.SmsSendReport
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.repository.MessageRepository
import ir.rosheno.launcher.domain.repository.SmsSource
import ir.rosheno.launcher.domain.repository.SmsTransport
import ir.rosheno.launcher.domain.sms.smsPartCount

class SmsRepository(val source: SmsSource, val transport: SmsTransport, val journal: SmsSendJournal) : MessageRepository {
    constructor(context: Context) : this(PhoneSmsSource(context), AndroidSmsTransport(context), SmsSendJournal(context))
    override fun send(address: String, body: String): String {
        val number = normalizePhone(address) ?: error("INVALID_NUMBER")
        require(body.isNotBlank() && body.length <= 5000)
        val parts = smsPartCount(body)
        val token = journal.begin(parts)
        try { transport.submit(number, body, token, parts) }
        catch (error: Exception) { journal.failPending(token); throw error }
        return token
    }

    override fun load(limit: Int): MessagePage = source.load(limit)
    override fun readReport(): SmsSendReport? = journal.read()
    override fun observeReports(onChange: () -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange() }
        journal.preferences.registerOnSharedPreferenceChangeListener(listener)
        return { journal.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
