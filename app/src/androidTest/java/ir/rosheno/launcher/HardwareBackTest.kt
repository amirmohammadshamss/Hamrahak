package ir.rosheno.launcher

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HardwareBackTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun back() {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    @Test fun hardwareBackReturnsFromEveryHomePageAndRestoresHistoryAfterRotation() {
        val store = ConfigStore(ApplicationProvider.getApplicationContext())
        val original = store.read()
        store.save(LauncherConfig(setupComplete = true))
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("کمک می‌خواهم").fetchSemanticsNodes().isNotEmpty() }
            listOf("تماس", "مخاطبین", "خانوادهٔ من", "پیام‌ها", "عکس‌ها", "برنامه‌ها", "دوربین").forEach { title ->
                compose.onNodeWithText(title).performScrollTo().performClick()
                compose.onNodeWithTag("page-header").assertIsDisplayed()
                back()
                compose.onNodeWithText("کمک می‌خواهم").assertExists()
            }
            compose.onNodeWithText("مخاطبین").performScrollTo().performClick()
            compose.onNodeWithText("افزودن مخاطب جدید").performClick()
            compose.onNodeWithText("مخاطب جدید").assertExists()
            back()
            compose.onNodeWithText("افزودن مخاطب جدید").assertExists()
            compose.onNodeWithText("تنظیمات").performClick()
            scenario.recreate()
            compose.onNodeWithText("تنظیمات روشنو").assertExists()
            back()
            compose.onNodeWithText("افزودن مخاطب جدید").assertExists()
            back()
            compose.onNodeWithText("کمک می‌خواهم").assertExists()
            back()
            compose.onNodeWithText("کمک می‌خواهم").assertExists()
        } finally { scenario.close(); store.save(original) }
    }

    @Test fun guideStepsAndCameraReturnOneLevelAtATime() {
        val store = ConfigStore(ApplicationProvider.getApplicationContext())
        val original = store.read()
        store.save(LauncherConfig(setupComplete = true))
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("کمک می‌خواهم").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("عکس‌ها").performScrollTo().performClick()
            compose.onNodeWithText("گرفتن عکس تازه").performScrollTo().performClick()
            back()
            compose.onNodeWithText("عکس‌های من").assertExists()
            back()
            compose.onNodeWithText("راهنمای استفاده").performScrollTo().performClick()
            compose.onNodeWithText("تماس با خانواده").performScrollTo().performClick()
            compose.onNodeWithText("مرحلهٔ بعد").performScrollTo().performClick()
            back()
            compose.onNodeWithText("خانواده را پیدا کنید").assertExists()
            back()
            compose.onNodeWithText("چه کاری می‌خواهید انجام دهید؟").assertExists()
            back()
            compose.onNodeWithText("کمک می‌خواهم").assertExists()
        } finally { scenario.close(); store.save(original) }
    }
}
