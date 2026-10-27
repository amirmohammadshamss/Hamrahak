package ir.rosheno.launcher.domain.sms

import ir.rosheno.launcher.domain.model.MessageThread
import ir.rosheno.launcher.domain.model.TextMessage

fun messageThreads(messages: List<TextMessage>): List<MessageThread> = messages.groupBy { it.thread }
    .map { (id, rows) -> rows.sortedByDescending { it.date }.let { MessageThread(id, it.first().address, it) } }
    .sortedByDescending { it.messages.first().date }

private val gsmBasic = ("@£" + '$' + "¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà").toSet()
private val gsmExtended = "\u000C^{}\\[~]|€".toSet()

/** Standard GSM-7 / UTF-16 payload boundaries, with no SIM-identity access. */
fun splitSmsText(body: String): ArrayList<String> {
    if (body.isEmpty()) return arrayListOf()
    val gsm = body.all { it in gsmBasic || it in gsmExtended }
    val units = if (gsm) body.fold(0) { total, character -> total + if (character in gsmExtended) 2 else 1 } else body.length
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
