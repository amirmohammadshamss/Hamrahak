package ir.rosheno.launcher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFeatureTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var app: IsolatedTestApp
    private val owner = ViewModelStore()
    @After fun cleanup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { owner.clear() }
        if (::app.isInitialized) app.cleanup()
    }
    private fun show() {
        app = IsolatedTestApp(ApplicationProvider.getApplicationContext())
        ConfigStore(app).save(LauncherConfig(contacts = listOf(FamilyContact("fixture", "Family fixture", "09121234567")),
            helpContactId = "fixture", setupComplete = true))
        lateinit var vm: LauncherViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            vm = ViewModelProvider(owner, ViewModelProvider.AndroidViewModelFactory(app))[LauncherViewModel::class.java]
        }
        compose.setContent { LauncherApp(vm, PlatformActions(compose.activity, vm::message, {}), 0, 0) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("کمک می‌خواهم").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("تنظیمات").performClick()
    }
    @Test fun textSizeAndFamilyDeleteRequireCorrectUserChoices() {
        show()
        compose.onNodeWithText("خیلی درشت").performScrollTo().performClick()
        compose.waitUntil(5_000) { ConfigStore(app).read().size == 2 }
        compose.onNodeWithText("حذف از روشنو").performScrollTo().performClick()
        compose.onNodeWithText("انصراف").performClick()
        assertEquals(1, ConfigStore(app).read().contacts.size)
        compose.onNodeWithText("حذف از روشنو").performScrollTo().performClick()
        compose.onNodeWithText("حذف").performClick()
        compose.waitUntil(5_000) { ConfigStore(app).read().contacts.isEmpty() }
        assertNull(ConfigStore(app).read().helpContactId)
        compose.onNodeWithText("خانه").performClick()
        compose.onNodeWithText("کمک می‌خواهم").performClick()
        compose.onNodeWithText("مخاطب کمک را انتخاب کنید").assertExists()
    }
    @Test fun resetCancelPreservesDataAndConfirmReturnsToSetup() {
        show()
        compose.onNodeWithText("بازنشانی تنظیمات روشنو").performScrollTo().performClick()
        compose.onNodeWithText("انصراف").performClick()
        assertTrue(ConfigStore(app).read().setupComplete)
        assertEquals(1, ConfigStore(app).read().contacts.size)
        compose.onNodeWithText("بازنشانی تنظیمات روشنو").performScrollTo().performClick()
        compose.onNodeWithText("پاک کردن تنظیمات").performClick()
        compose.waitUntil(5_000) { !ConfigStore(app).read().setupComplete }
        compose.onNodeWithText("به روشنو خوش آمدید").assertExists()
        assertEquals(LauncherConfig(), ConfigStore(app).read())
    }
}
