package ir.hamrahak.launcher

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneAndStoreTest {
    @Test fun draftPhotosAreBoundedAndCannotDeleteCommittedOrUnrelatedFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = java.io.File(context.cacheDir, "photo-test-${java.util.UUID.randomUUID()}.png")
        val bitmap = android.graphics.Bitmap.createBitmap(1200, 600, android.graphics.Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val photos = ContactPhotos(context)
        var staged: String? = null
        var committed: String? = null
        try {
            staged = photos.stage(android.net.Uri.fromFile(source))
            val decoded = android.graphics.BitmapFactory.decodeFile(staged)
            assertEquals(512, decoded.width)
            assertEquals(256, decoded.height)
            decoded.recycle()
            committed = photos.commit(staged)
            photos.discardDraft(committed)
            assertTrue(java.io.File(requireNotNull(committed)).exists())
            photos.deleteCommitted(source.absolutePath)
            assertTrue(source.exists())
            photos.discardDraft(staged)
            assertFalse(java.io.File(requireNotNull(staged)).exists())
        } finally {
            photos.discardDraft(staged)
            photos.deleteCommitted(committed)
            source.delete()
        }
    }
    @Test fun normalizesPersianAndArabicNumbers() {
        assertEquals("+989121234567", normalizePhone("+۹۸ (۹۱۲) ۱۲۳-۴۵۶۷"))
        assertEquals("09121234567", normalizePhone("٠٩١٢١٢٣٤٥٦٧"))
    }
    @Test fun rejectsUriCommandsAndServiceCodes() {
        listOf("", "12", "*123#", "0912;123", "tel:09121234567", "++98912", "123+456",
            "1234567890123456", "123\n456").forEach { assertNull(it, normalizePhone(it)) }
    }
    @Test fun configSurvivesNewStoreInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ConfigStore(context)
        val original = store.read()
        try {
            val config = LauncherConfig(
                contacts = listOf(FamilyContact("test", "مادر", "+989121234567", null)),
                favorites = listOf("test.package/.Main"), helpContactId = "test", size = 2, setupComplete = true)
            store.save(config)
            assertEquals(config, ConfigStore(context).read())
        } finally { store.save(original) }
    }
}

/** Run on a dedicated test install. No calls are placed and no Home role is changed. */
@RunWith(AndroidJUnit4::class)
class PrototypeJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun helpWithoutContactsGuidesSetupAndOpensCorrectDialerIntent() {
        val store = ConfigStore(ApplicationProvider.getApplicationContext())
        val original = store.read()
        store.save(LauncherConfig(setupComplete = true))
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val captured = java.util.concurrent.atomic.AtomicReference<android.content.Intent?>()
        val monitor = object : android.app.Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: android.content.Intent): android.app.Instrumentation.ActivityResult? {
                if (intent.action != android.content.Intent.ACTION_DIAL) return null
                captured.set(intent)
                // Observe handoff without opening a real phone app or placing any call.
                return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText("کمک می‌خواهم")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("کمک می‌خواهم").performClick()
            compose.onNodeWithText("مخاطب کمک را انتخاب کنید").assertIsDisplayed()
            compose.onNodeWithText("افزودن مخاطب کمک").performScrollTo().performClick()
            compose.onNodeWithText("نام").performTextInput("مخاطب کمک آزمایشی")
            compose.onNodeWithText("شماره تلفن").performTextInput("۰۹۱۲۱۲۳۴۵۶۷")
            compose.onNodeWithText("ذخیره").performClick()
            compose.waitUntil(5_000) { store.read().contacts.size == 1 }
            val id = store.read().contacts.single().id
            assertNull(store.read().helpContactId)
            compose.onNodeWithTag("choose-help-$id").performScrollTo().performClick()
            compose.waitUntil(5_000) { store.read().helpContactId == id }
            compose.onNodeWithText("09121234567").assertIsDisplayed()
            compose.onNodeWithText("انصراف").performClick()
            assertNull(captured.get())
            compose.onNodeWithText("کمک می‌خواهم").performClick()
            compose.onNodeWithText("رفتن به تلفن").performClick()
            compose.waitUntil(5_000) { captured.get() != null }
            assertEquals(android.content.Intent.ACTION_DIAL, captured.get()?.action)
            assertEquals("tel:09121234567", captured.get()?.dataString)
        } finally {
            scenario.close()
            instrumentation.removeMonitor(monitor)
            store.save(original)
        }
    }

    @Test fun deletedHelpContactRequiresExplicitReplacementAndSurvivesRecreation() {
        val store = ConfigStore(ApplicationProvider.getApplicationContext())
        val original = store.read()
        store.save(LauncherConfig(contacts = listOf(
            FamilyContact("first", "نفر اول", "09121234567"),
            FamilyContact("second", "نفر دوم", "09351234567")),
            helpContactId = "deleted", setupComplete = true))
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText("کمک می‌خواهم")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("کمک می‌خواهم").performClick()
            compose.onNodeWithText("مخاطب کمک را انتخاب کنید").assertIsDisplayed()
            compose.onNodeWithTag("choose-help-second").performScrollTo().performClick()
            compose.waitUntil(5_000) { store.read().helpContactId == "second" }
            compose.onNodeWithText("09351234567").assertIsDisplayed()
            scenario.recreate()
            compose.onNode(hasText("تماس با نفر دوم") and hasAnyAncestor(isDialog())).assertIsDisplayed()
            compose.onNodeWithText("09351234567").assertIsDisplayed()
            assertEquals("second", ConfigStore(ApplicationProvider.getApplicationContext()).read().helpContactId)
            compose.onNodeWithText("انصراف").performClick()
        } finally {
            scenario.close()
            store.save(original)
        }
    }

    @Test fun editsSurviveRecreationAndPreserveHelpContactWhenReordered() {
        val store = ConfigStore(ApplicationProvider.getApplicationContext())
        val original = store.read()
        store.save(LauncherConfig(contacts = listOf(
            FamilyContact("first", "مریم", "09121234567"),
            FamilyContact("second", "علی", "09351234567")), helpContactId = "first", setupComplete = true))
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodes(androidx.compose.ui.test.hasText("تماس سریع")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("تنظیمات").performClick()
            compose.onNodeWithTag("edit-contact-first").performScrollTo().performClick()
            compose.onNodeWithText("نام").performTextReplacement("مریم جدید")
            compose.onNodeWithText("شماره تلفن").performTextReplacement("۰۹۱۹۱۲۳۴۵۶۷")
            scenario.recreate()
            compose.onNodeWithText("مریم جدید").assertIsDisplayed()
            compose.onNodeWithText("ذخیره").performClick()
            compose.waitUntil(5_000) { store.read().contacts.first().name == "مریم جدید" }
            assertEquals("09191234567", store.read().contacts.first().number)
            assertEquals("first", store.read().helpContactId)
            compose.onNodeWithTag("contact-down-first").performScrollTo().performClick()
            compose.waitUntil(5_000) { store.read().contacts.first().id == "second" }
            compose.onNodeWithTag("edit-contact-first").performScrollTo().performClick()
            compose.onNodeWithText("نام").performTextReplacement("تغییر لغوشده")
            compose.onNodeWithText("انصراف").performClick()
            assertEquals("مریم جدید", store.read().contacts.last().name)
            compose.onNodeWithText("خانه").performClick()
            compose.onNodeWithText("کمک می‌خواهم").assertIsDisplayed()
            compose.onNodeWithText("مریم جدید").performScrollTo().performClick()
            compose.onNode(hasText("تماس با مریم جدید") and hasAnyAncestor(isDialog())).assertIsDisplayed()
            compose.onNodeWithText("09191234567").assertIsDisplayed()
            assertEquals("first", store.read().helpContactId)
        } finally {
            scenario.close()
            store.save(original)
        }
    }

    @Test fun setupContactConfirmationAndReturnHome() {
        val store = ConfigStore(ApplicationProvider.getApplicationContext())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsComponent = requireNotNull(context.packageManager.getLaunchIntentForPackage("com.android.settings")?.component)
        val settingsKey = settingsComponent.flattenToString()
        val settingsLabel = context.packageManager.getActivityInfo(settingsComponent, 0).loadLabel(context.packageManager).toString()
        val original = store.read()
        store.save(LauncherConfig())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodes(androidx.compose.ui.test.hasText("شروع استفاده")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("خیلی درشت").performScrollTo().performClick()
            compose.onNodeWithText("شروع استفاده").performScrollTo().performClick()
            compose.onNodeWithText("خانوادهٔ من").performScrollTo().performClick()
            compose.onNodeWithText("افزودن یک نفر").performScrollTo().performClick()
            compose.onNodeWithText("نام").performTextInput("خانواده آزمایشی")
            compose.onNodeWithText("شماره تلفن").performTextInput("۰۹۱۲۱۲۳۴۵۶۷")
            compose.onNodeWithText("ذخیره").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(androidx.compose.ui.test.hasText("خانواده آزمایشی")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("خانواده آزمایشی").performScrollTo().performClick()
            compose.onNodeWithText("تماس با خانواده آزمایشی").assertIsDisplayed()
            compose.onNodeWithText("09121234567").assertIsDisplayed()
            compose.onNodeWithText("انصراف").performClick()
            compose.onNodeWithText("بازگشت به خانه").performScrollTo().performClick()
            compose.onNodeWithText("خانوادهٔ من").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("برنامه‌ها").performScrollTo().performClick()
            compose.onNodeWithText("جست‌وجوی برنامه").performTextInput(settingsLabel)
            compose.onNodeWithTag("app-$settingsKey").performScrollTo().assertIsDisplayed()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("app-icon-$settingsKey"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("app-icon-$settingsKey", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("افزودن به دلخواه").performScrollTo().performClick()
            compose.waitUntil(5_000) { settingsKey in store.read().favorites }
            compose.onNodeWithText("بازگشت به خانه").performScrollTo().performClick()
            compose.onAllNodesWithText(settingsLabel).onLast().performScrollTo().assertIsDisplayed()
            // Help stays reachable even at the end of a long home screen.
            compose.onNodeWithText("کمک می‌خواهم").assertIsDisplayed()
        } finally {
            scenario.close()
            store.save(original)
        }
    }
}
