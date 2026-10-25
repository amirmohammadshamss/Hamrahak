package ir.hamrahak.launcher

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoAndGuideTest {
    @get:Rule val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun capturePhotoViewAndReturnToLauncher() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ConfigStore(context)
        val original = store.read()
        val before = context.filesDir.resolve("captures").listFiles()?.map { it.name }?.toSet().orEmpty()
        store.save(LauncherConfig(setupComplete = true))
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) { compose.onAllNodes(hasText("کمک می‌خواهم")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("دوربین").performScrollTo().performClick()
            compose.onNodeWithText("بازگشت به همراهک").assertIsDisplayed()
            compose.waitUntil(30_000) {
                compose.onAllNodes(hasTestTag("camera-shutter") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("camera-shutter").performClick()
            compose.waitUntil(20_000) {
                compose.onAllNodes(hasText("عکس‌های من")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("نمایش عکس")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("نمایش عکس").assertIsDisplayed()
            val captured = context.filesDir.resolve("captures").listFiles().orEmpty().filter { it.name !in before }
            assertEquals(1, captured.size)
            assertTrue(captured.single().length() > 0)
            compose.onNodeWithText("بازگشت به همراهک").performClick()
            compose.onNodeWithText("کمک می‌خواهم").assertIsDisplayed()
            compose.onNodeWithText("عکس‌ها").performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("عکس ذخیره‌شده")).fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithContentDescription("عکس ذخیره‌شده").onFirst().performScrollTo().performClick()
            compose.onNodeWithText("بازگشت به همراهک").assertIsDisplayed()
            compose.onNodeWithText("بازگشت به همراهک").performClick()
            compose.onNodeWithText("کمک می‌خواهم").assertIsDisplayed()
        } finally {
            scenario.close()
            context.filesDir.resolve("captures").listFiles().orEmpty().filter { it.name !in before }.forEach { it.delete() }
            store.save(original)
        }
    }

    @Test fun illustratedGuideHasTapPicturesAndNextSteps() {
        val store = ConfigStore(ApplicationProvider.getApplicationContext())
        val original = store.read()
        store.save(LauncherConfig(setupComplete = true))
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) { compose.onAllNodes(hasText("کمک می‌خواهم")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("راهنمای استفاده").performScrollTo().performClick()
            compose.onNodeWithText("راهنمای تصویری").assertIsDisplayed()
            compose.onNodeWithText("عکس گرفتن و برگشتن").performScrollTo().performClick()
            compose.onNodeWithTag("guide-illustration").assertIsDisplayed()
            compose.onNodeWithText("مرحلهٔ بعد").performScrollTo().performClick()
            compose.onNodeWithText("عکس بگیرید").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("مرحلهٔ بعد").performScrollTo().performClick()
            compose.onNodeWithText("به همراهک برگردید").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("خانه").performClick()
            compose.onNodeWithText("کمک می‌خواهم").assertIsDisplayed()
        } finally { scenario.close(); store.save(original) }
    }
}
