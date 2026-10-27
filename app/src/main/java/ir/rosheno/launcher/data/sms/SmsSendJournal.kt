package ir.rosheno.launcher.data.sms

import android.content.Context
import android.telephony.SmsManager
import ir.rosheno.launcher.domain.model.SmsSendReport
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

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
