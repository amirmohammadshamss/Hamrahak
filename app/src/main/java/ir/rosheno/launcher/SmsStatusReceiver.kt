package ir.rosheno.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.rosheno.launcher.data.sms.SmsSendJournal

class SmsStatusReceiver : BroadcastReceiver() {
    companion object { const val ACTION = "ir.rosheno.launcher.SMS_SENT" }
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
