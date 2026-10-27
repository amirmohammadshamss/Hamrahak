package ir.rosheno.launcher

import ir.rosheno.launcher.presentation.common.resolve
import android.Manifest
import android.app.Activity
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import ir.rosheno.launcher.data.sms.SmsRepository
import ir.rosheno.launcher.data.sms.SmsSendJournal
import ir.rosheno.launcher.domain.model.MessagePage
import ir.rosheno.launcher.domain.model.SmsSendReport
import ir.rosheno.launcher.domain.model.TextMessage
import ir.rosheno.launcher.domain.phone.comparableSmsAddress
import ir.rosheno.launcher.domain.repository.SmsSource
import ir.rosheno.launcher.domain.repository.SmsTransport
import ir.rosheno.launcher.domain.sms.messageThreads
import ir.rosheno.launcher.presentation.sms.SmsViewModel
import java.util.concurrent.atomic.AtomicInteger
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmsStateTest {
    @get:Rule val permissions = GrantPermissionRule.grant(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS)
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var app: IsolatedTestApp
    private val owners = mutableListOf<ViewModelStore>()
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    @Before fun prepare() { app = IsolatedTestApp(ApplicationProvider.getApplicationContext()) }
    @After fun cleanup() { main { owners.forEach { it.clear() } }; app.cleanup() }
    private fun vm(source: SmsSource, transport: SmsTransport, saved: SavedStateHandle = SavedStateHandle()): SmsViewModel {
        lateinit var result: SmsViewModel
        main {
            val owner = ViewModelStore().also { owners += it }
            result = ViewModelProvider(owner, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SmsViewModel(app, saved, SmsRepository(source, transport, SmsSendJournal(app))) as T
            })[SmsViewModel::class.java]
        }
        return result
    }
    private val empty = object : SmsSource { override fun load(limit: Int) = MessagePage(emptyList(), false) }
    private val noSend = object : SmsTransport { override fun submit(address: String, body: String, token: String, parts: Int) { error("Unexpected send") } }

    @Test fun failedReadCanRetryAndLoadOlderMessages() {
        val count = AtomicInteger()
        val limitSeen = AtomicInteger()
        val source = object : SmsSource {
            override fun load(limit: Int): MessagePage {
                limitSeen.set(limit)
                if (count.incrementAndGet() == 1) error("Provider unavailable")
                return MessagePage(listOf(TextMessage(1, 1, "09121234567", "fixture", 1, 1)), true)
            }
        }
        val model = vm(source, noSend)
        main { model.start() }
        compose.waitUntil(5_000) { model.state.value.error != null }
        main { model.refresh() }
        compose.waitUntil(5_000) { model.state.value.messages.size == 1 }
        assertNull(model.state.value.error)
        main { model.more() }
        compose.waitUntil(5_000) { limitSeen.get() == 400 && !model.state.value.loading }
        assertTrue(model.state.value.hasMore)
    }
    @Test fun pendingSendBlocksDuplicatesAndLateSuccessPreservesNewDraft() {
        val calls = AtomicInteger()
        val transport = object : SmsTransport { override fun submit(address: String, body: String, token: String, parts: Int) { calls.incrementAndGet() } }
        val model = vm(empty, transport)
        main { model.draft("09121234567", "first draft"); model.sendConfirmed("09121234567", "first draft") }
        compose.waitUntil(5_000) { model.state.value.report != null && !model.state.value.submitting }
        main { model.sendConfirmed("09121234567", "first draft"); model.draft(text = "new draft") }
        assertEquals(1, calls.get())
        val report = requireNotNull(model.state.value.report)
        SmsSendJournal(app).record(report.token, 0, Activity.RESULT_OK)
        compose.waitUntil(5_000) { model.state.value.report?.sent == true }
        assertEquals("new draft", model.state.value.draft)
    }
    @Test fun missingSimFailsExplicitlyAndRetainsDraft() {
        val transport = object : SmsTransport { override fun submit(address: String, body: String, token: String, parts: Int) { error("NO_DEFAULT_SIM") } }
        val model = vm(empty, transport)
        main { model.draft("09121234567", "keep this"); model.sendConfirmed("09121234567", "keep this") }
        compose.waitUntil(5_000) { model.state.value.error != null && !model.state.value.submitting }
        assertTrue(requireNotNull(model.state.value.error).resolve(app).contains("سیم‌کارت پیش‌فرض"))
        assertEquals("keep this", model.state.value.draft)
        assertTrue(requireNotNull(SmsSendJournal(app).read()).failed)
    }
    @Test fun draftStateIsBoundedAndRestoredAndInvalidSubmissionsNeverReachTransport() {
        val saved = SavedStateHandle()
        val model = vm(empty, noSend, saved)
        main { model.draft("1".repeat(50), "س".repeat(6000)) }
        val restored = vm(empty, noSend, saved).state.value
        assertEquals(32, restored.recipient.length)
        assertEquals(5000, restored.draft.length)
        val repository = SmsRepository(empty, noSend, SmsSendJournal(app))
        listOf("*123#" to "text", "09121234567" to " ", "09121234567" to "a".repeat(5001)).forEach { (number, body) ->
            assertTrue(runCatching { repository.send(number, body) }.isFailure)
        }
        assertNull(repository.journal.read())
    }
    @Test fun journalRejectsUnknownCallbacksAndBoundsHistory() {
        val journal = SmsSendJournal(app)
        val token = journal.begin(2)
        journal.record("unknown", 0, Activity.RESULT_OK)
        journal.record(token, -1, Activity.RESULT_OK)
        journal.record(token, 2, Activity.RESULT_OK)
        assertEquals(listOf(null, null), journal.read()?.results)
        journal.record(token, 0, Activity.RESULT_OK)
        journal.failPending(token)
        assertTrue(requireNotNull(journal.read()).failed)
        assertEquals(Activity.RESULT_OK, journal.read()?.results?.first())
        repeat(25) { journal.begin(1) }
        assertTrue(journal.preferences.all.keys.count { it.startsWith("job-") } <= 20)
        assertTrue(SmsSendReport("expired", 1, listOf(null), 0).expired)
    }
    @Test fun conversationsAndMessagesSortByNewestAndNormalizeFamilyAddresses() {
        val threads = messageThreads(listOf(TextMessage(1, 7, "first", "old", 1, 1),
            TextMessage(2, 8, "second", "newest", 3, 1), TextMessage(3, 7, "latest first", "new", 2, 2)))
        assertEquals(listOf(8L, 7L), threads.map { it.id })
        assertEquals(listOf(3L, 1L), threads.last().messages.map { it.id })
        assertEquals("latest first", threads.last().address)
        assertEquals(comparableSmsAddress("+989121234567"), comparableSmsAddress("۰۹۱۲۱۲۳۴۵۶۷"))
    }
}
