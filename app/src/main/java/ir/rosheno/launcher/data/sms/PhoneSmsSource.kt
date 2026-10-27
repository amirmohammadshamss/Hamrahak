package ir.rosheno.launcher.data.sms

import android.content.Context
import android.provider.Telephony
import ir.rosheno.launcher.domain.model.MessagePage
import ir.rosheno.launcher.domain.model.TextMessage
import ir.rosheno.launcher.domain.repository.SmsSource

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
