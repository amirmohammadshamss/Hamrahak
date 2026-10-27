package ir.rosheno.launcher.domain

import ir.rosheno.launcher.domain.model.TextMessage
import ir.rosheno.launcher.domain.phone.comparableSmsAddress
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.sms.messageThreads
import ir.rosheno.launcher.domain.sms.splitSmsText
import org.junit.Assert.*
import org.junit.Test

class PhoneAndSmsRulesTest {
    @Test fun normalizesPersianAndArabicDigitsAndRejectsCommands() {
        assertEquals("+989121234567", normalizePhone(" +۹۸ (۹۱۲) ۱۲۳-۴۵۶۷ "))
        assertEquals("09121234567", normalizePhone("٠٩١٢١٢٣٤٥٦٧"))
        listOf("*123#", "tel:09121234567", "0912;123", "++98123").forEach { assertNull(normalizePhone(it)) }
        assertEquals(comparableSmsAddress("+989121234567"), comparableSmsAddress("۰۹۱۲۱۲۳۴۵۶۷"))
    }

    @Test fun splitsSmsAtEncodingBoundariesWithoutBreakingSurrogatePairs() {
        assertEquals(1, splitSmsText("a".repeat(160)).size)
        assertEquals(listOf(153, 8), splitSmsText("a".repeat(161)).map { it.length })
        assertEquals(1, splitSmsText("^".repeat(80)).size)
        assertEquals(2, splitSmsText("^".repeat(81)).size)
        assertEquals(listOf(67, 4), splitSmsText("س".repeat(71)).map { it.length })
        val body = "س".repeat(66) + "😀" + "س".repeat(10)
        val parts = splitSmsText(body)
        assertEquals(body, parts.joinToString(""))
        assertTrue(parts.all { it.length <= 67 && !it.last().isHighSurrogate() && !it.first().isLowSurrogate() })
    }

    @Test fun groupsConversationsUsingNewestMessageAndAddress() {
        val threads = messageThreads(listOf(
            TextMessage(1, 7, "old address", "old", 1, 1),
            TextMessage(2, 8, "other", "newest", 3, 1),
            TextMessage(3, 7, "latest address", "new", 2, 2),
        ))
        assertEquals(listOf(8L, 7L), threads.map { it.id })
        assertEquals(listOf(3L, 1L), threads.last().messages.map { it.id })
        assertEquals("latest address", threads.last().address)
    }
}
