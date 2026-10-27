package ir.rosheno.launcher.domain.model

/** Android SMS transport maps successful part callbacks to this result code. */
const val SMS_RESULT_OK = -1

data class SmsSendReport(val token: String, val parts: Int, val results: List<Int?>, val created: Long) {
    val failed: Boolean get() = results.any { it != null && it != SMS_RESULT_OK }
    val complete: Boolean get() = results.all { it != null }
    val sent: Boolean get() = complete && !failed
    val expired: Boolean get() = !complete && System.currentTimeMillis() - created > 300_000
}
