package ir.hamrahak.launcher

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class SmsFeatureTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS)
    @get:Rule val compose = createComposeRule()

    @Test fun readsRealProviderWithPermissionWithoutChangingMessages() {
        val page = PhoneSmsSource(ApplicationProvider.getApplicationContext()).load(20)
        assertTrue(page.messages.size <= 20)
        assertTrue(page.messages.zipWithNext().all { (first, second) -> first.date >= second.date })
    }

    @Test fun splitsPersianGsmExtensionAndEmojiWithoutBreakingText() {
        assertEquals(1, smsPartCount("a".repeat(160)))
        assertEquals(2, smsPartCount("a".repeat(161)))
        assertEquals(1, smsPartCount("^".repeat(80)))
        assertEquals(2, smsPartCount("^".repeat(81)))
        assertEquals(1, smsPartCount("س".repeat(70)))
        assertEquals(2, smsPartCount("س".repeat(71)))
        val emoji = "\uD83D\uDE00".repeat(100)
        val parts = splitSmsText(emoji)
        assertEquals(emoji, parts.joinToString(""))
        assertTrue(parts.all { it.length <= 67 && !it.last().isHighSurrogate() && !it.first().isLowSurrogate() })
    }

    @Test fun partialFailureSurvivesReloadAndDoesNotBecomeSuccessOnDuplicateCallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "sms-test-${UUID.randomUUID()}"
        try {
            val journal = SmsSendJournal(context, name)
            val id = journal.begin(3)
            journal.record(id, 2, Activity.RESULT_OK)
            journal.record(id, 0, Activity.RESULT_OK)
            assertFalse(requireNotNull(journal.read()).complete)
            journal.record(id, 1, android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE)
            journal.record(id, 1, Activity.RESULT_OK)
            val restored = requireNotNull(SmsSendJournal(context, name).read())
            assertTrue(restored.complete)
            assertTrue(restored.failed)
            assertFalse(restored.sent)
            assertTrue(smsReportLabel(restored).contains("بخشی"))
            assertTrue(smsPartCount("سلام ".repeat(60)) > 1)
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun inboxFullMessageReplyCancelAndConfirmedSendUseFakeTransportOnly() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val name = "sms-test-${UUID.randomUUID()}"
        val journal = SmsSendJournal(app, name)
        val source = object : SmsSource {
            override fun load(limit: Int) = MessagePage(listOf(
                TextMessage(2, 7, "+989121234567", "پاسخ قبلی", 2000, 2),
                TextMessage(1, 7, "09121234567", "سلام، این یک پیام آزمایشی است", 1000, 1)), false)
        }
        val calls = AtomicInteger()
        var sentAddress = ""
        var sentBody = ""
        val transport = object : SmsTransport {
            override fun submit(address: String, body: String, token: String, parts: Int) {
                sentAddress = address; sentBody = body; calls.incrementAndGet()
                repeat(parts) { journal.record(token, it, Activity.RESULT_OK) }
            }
        }
        val owner = ViewModelStore()
        val vm = ViewModelProvider(owner, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SmsViewModel(app, SavedStateHandle(), SmsRepository(source, transport, journal)) as T
        })[SmsViewModel::class.java]
        try {
            compose.setContent { HamrahTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SmsScreen(listOf(FamilyContact("family", "مریم", "09121234567")), 0, {}, {}, vm)
            } } }
            compose.waitUntil(10_000) { compose.onAllNodes(hasText("مریم")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("مریم").performClick()
            compose.onNodeWithText("سلام، این یک پیام آزمایشی است").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("پاسخ دادن").performClick()
            compose.onNodeWithText("متن پیام").performTextInput("پاسخ آزمایشی")
            compose.onNodeWithText("بررسی و ارسال").performScrollTo().performClick()
            compose.onNodeWithText("این پیامک فرستاده شود؟").assertIsDisplayed()
            compose.onNodeWithText("انصراف").performClick()
            assertEquals(0, calls.get())
            assertEquals("پاسخ آزمایشی", vm.state.value.draft)
            compose.onNodeWithText("بررسی و ارسال").performScrollTo().performClick()
            compose.onNodeWithText("ارسال پیامک").performClick()
            compose.waitUntil(5_000) { vm.state.value.report?.sent == true && vm.state.value.draft.isEmpty() }
            assertEquals(1, calls.get())
            assertEquals("+989121234567", sentAddress)
            assertEquals("پاسخ آزمایشی", sentBody)
            compose.onNodeWithText("پیامک ارسال شد.").assertIsDisplayed()
        } finally { owner.clear(); app.deleteSharedPreferences(name) }
    }
}
