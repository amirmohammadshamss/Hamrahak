package ir.rosheno.launcher.data.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import ir.rosheno.launcher.SmsStatusReceiver
import ir.rosheno.launcher.domain.repository.SmsTransport
import ir.rosheno.launcher.domain.sms.splitSmsText

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
                .setData(Uri.parse("rosheno-internal://sms/$token/$index"))
                .putExtra("token", token).putExtra("part", index)
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        })
        manager.sendMultipartTextMessage(address, null, pieces, receipts, null)
    }
}
