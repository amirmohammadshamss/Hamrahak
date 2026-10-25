package ir.hamrahak.launcher

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TextMessage(val id: Long, val thread: Long, val address: String, val body: String,
    val date: Long, val type: Int)
data class MessagePage(val messages: List<TextMessage>, val hasMore: Boolean)
data class MessageThread(val id: Long, val address: String, val messages: List<TextMessage>)

fun messageThreads(messages: List<TextMessage>): List<MessageThread> = messages.groupBy { it.thread }
    .map { (id, rows) -> rows.sortedByDescending { it.date }.let { MessageThread(id, it.first().address, it) } }
    .sortedByDescending { it.messages.first().date }

private val gsmBasic = ("@£" + '$' + "¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà").toSet()
private val gsmExtended = "\u000C^{}\\[~]|€".toSet()

/** Standard GSM-7 / UTF-16 payload boundaries, with no SIM-identity access. */
fun splitSmsText(body: String): ArrayList<String> {
    if (body.isEmpty()) return arrayListOf()
    val gsm = body.all { it in gsmBasic || it in gsmExtended }
    val units = if (gsm) body.sumOf { if (it in gsmExtended) 2 else 1 } else body.length
    if (units <= if (gsm) 160 else 70) return arrayListOf(body)
    val capacity = if (gsm) 153 else 67
    val chunks = arrayListOf<String>()
    var start = 0; var index = 0; var used = 0
    while (index < body.length) {
        val width = if (!gsm && body[index].isHighSurrogate() && index + 1 < body.length && body[index + 1].isLowSurrogate()) 2 else 1
        val cost = if (gsm) if (body[index] in gsmExtended) 2 else 1 else width
        if (used + cost > capacity) { chunks += body.substring(start, index); start = index; used = 0 }
        used += cost; index += width
    }
    chunks += body.substring(start)
    return chunks
}
fun smsPartCount(body: String): Int = splitSmsText(body).size

interface SmsSource { fun load(limit: Int): MessagePage }
class PhoneSmsSource(private val context: Context) : SmsSource {
    override fun load(limit: Int): MessagePage {
        val messages = mutableListOf<TextMessage>()
        val columns = arrayOf("_id", "thread_id", "address", "body", "date", "type")
        val cursor = context.contentResolver.query(Telephony.Sms.CONTENT_URI, columns, null, null, "date DESC")
            ?: error("SMS provider unavailable")
        cursor.use { c ->
            while (c.moveToNext()) {
                if (messages.size >= limit) return MessagePage(messages, true)
                messages += TextMessage(c.getLong(0), c.getLong(1), c.getString(2).orEmpty(),
                    c.getString(3).orEmpty(), c.getLong(4), c.getInt(5))
            }
        }
        return MessagePage(messages, false)
    }
}

data class SmsSendReport(val token: String, val parts: Int, val results: List<Int?>, val created: Long) {
    val failed: Boolean get() = results.any { it != null && it != Activity.RESULT_OK }
    val complete: Boolean get() = results.all { it != null }
    val sent: Boolean get() = complete && !failed
    val expired: Boolean get() = !complete && System.currentTimeMillis() - created > 300_000
}

/** Store result metadata only: no phone number or message text is duplicated here. */
class SmsSendJournal(context: Context, name: String = "sms-send-results") {
    val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    companion object { private val lock = Any() }

    fun begin(parts: Int): String = synchronized(lock) {
        require(parts in 1..100)
        val token = UUID.randomUUID().toString()
        val json = JSONObject().put("parts", parts).put("created", System.currentTimeMillis())
            .put("results", JSONArray().apply { repeat(parts) { put(JSONObject.NULL) } })
        val edit = preferences.edit().putString("job-$token", json.toString()).putString("last", token)
        // Bound historical metadata. Tokens are random and contain no personal data.
        preferences.all.keys.filter { it.startsWith("job-") }.sortedBy {
            runCatching { JSONObject(preferences.getString(it, "{}").orEmpty()).optLong("created") }.getOrDefault(0)
        }.dropLast(19).forEach { edit.remove(it) }
        check(edit.commit())
        token
    }
    fun record(token: String, part: Int, result: Int) = synchronized(lock) {
        val raw = preferences.getString("job-$token", null) ?: return@synchronized
        val json = JSONObject(raw)
        val values = json.getJSONArray("results")
        if (part !in 0 until values.length() || !values.isNull(part)) return@synchronized
        values.put(part, result)
        check(preferences.edit().putString("job-$token", json.toString()).commit())
    }
    fun failPending(token: String) = synchronized(lock) {
        val report = read(token) ?: return@synchronized
        report.results.indices.forEach { record(token, it, SmsManager.RESULT_ERROR_GENERIC_FAILURE) }
    }
    fun read(token: String? = preferences.getString("last", null)): SmsSendReport? = synchronized(lock) {
        if (token == null) return@synchronized null
        runCatching {
            val json = JSONObject(preferences.getString("job-$token", null) ?: return@synchronized null)
            val values = json.getJSONArray("results")
            SmsSendReport(token, json.getInt("parts"), (0 until values.length()).map {
                if (values.isNull(it)) null else values.getInt(it)
            }, json.getLong("created"))
        }.getOrNull()
    }
}

class SmsStatusReceiver : BroadcastReceiver() {
    companion object { const val ACTION = "ir.hamrahak.launcher.SMS_SENT" }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val token = intent.getStringExtra("token") ?: return
        val part = intent.getIntExtra("part", -1)
        val result = resultCode
        val pending = goAsync()
        java.util.concurrent.Executors.newSingleThreadExecutor().let { executor ->
            executor.execute {
                try { runCatching { SmsSendJournal(context).record(token, part, result) } }
                finally { pending.finish(); executor.shutdown() }
            }
        }
    }
}

interface SmsTransport { fun submit(address: String, body: String, token: String, parts: Int) }
class AndroidSmsTransport(private val context: Context) : SmsTransport {
    @Suppress("DEPRECATION")
    override fun submit(address: String, body: String, token: String, parts: Int) {
        val subscription = SubscriptionManager.getDefaultSmsSubscriptionId()
        // API 28 lacks isValidSubscriptionId; negative and reserved-default IDs are not usable.
        check(subscription >= 0 && subscription != Int.MAX_VALUE) { "NO_DEFAULT_SIM" }
        val manager = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(SmsManager::class.java)
            .createForSubscriptionId(subscription) else SmsManager.getSmsManagerForSubscriptionId(subscription)
        val pieces = splitSmsText(body)
        check(pieces.size == parts)
        val receipts = ArrayList(pieces.indices.map { index ->
            val intent = Intent(context, SmsStatusReceiver::class.java).setAction(SmsStatusReceiver.ACTION)
                .setData(Uri.parse("hamrahak-internal://sms/$token/$index"))
                .putExtra("token", token).putExtra("part", index)
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        })
        manager.sendMultipartTextMessage(address, null, pieces, receipts, null)
    }
}

class SmsRepository(val source: SmsSource, val transport: SmsTransport, val journal: SmsSendJournal) {
    constructor(context: Context) : this(PhoneSmsSource(context), AndroidSmsTransport(context), SmsSendJournal(context))
    fun send(address: String, body: String): String {
        val number = normalizePhone(address) ?: error("INVALID_NUMBER")
        require(body.isNotBlank() && body.length <= 5000)
        val parts = smsPartCount(body)
        val token = journal.begin(parts)
        try { transport.submit(number, body, token, parts) }
        catch (error: Exception) { journal.failPending(token); throw error }
        return token
    }
}
