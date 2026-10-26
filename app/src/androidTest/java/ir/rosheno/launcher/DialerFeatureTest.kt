package ir.rosheno.launcher

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class DialerFeatureTest {
    @get:Rule val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CALL_PHONE)
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun unsafeInputIsRejectedAndEmergencyOnlyOpensSystemDialer() {
        listOf("*123#", "tel:09121234567", "0912;123").forEach { assertNull(userCallIntent(it)) }
        assertNotEquals(Intent.ACTION_CALL, userCallIntent("12")?.action)
        val ordinary = requireNotNull(userCallIntent("۰۹۱۲۱۲۳۴۵۶۷"))
        assertEquals(Intent.ACTION_CALL, ordinary.action)
        assertEquals("tel:09121234567", ordinary.dataString)
        listOf("۱۱۰", "۱۱۲", "۱۱۵", "۱۲۵", "۹۱۱").forEach {
            assertEquals(Intent.ACTION_DIAL, requireNotNull(userCallIntent(it)).action)
        }
    }

    @Test fun keypadPersistsAfterRecreationAndOnlyConfirmedCallIsDispatched() {
        val store = ConfigStore(ApplicationProvider.getApplicationContext())
        val original = store.read()
        store.save(LauncherConfig(setupComplete = true, size = 2))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intercepted = CopyOnWriteArrayList<Intent>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CALL && intent.action != Intent.ACTION_DIAL) return null
                // Block before Android launches any phone activity: no call can be placed.
                intercepted += intent
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(10_000) { compose.onAllNodes(hasText("کمک می‌خواهم")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("تماس").performScrollTo().performClick()
            compose.onNodeWithText("شماره‌گیر").assertIsDisplayed()
            assertTrue(compose.onNodeWithTag("dial-key-1").getUnclippedBoundsInRoot().left <
                compose.onNodeWithTag("dial-key-3").getUnclippedBoundsInRoot().left)
            "09121234567".forEach { digit -> compose.onNodeWithTag("dial-key-$digit").performScrollTo().performClick() }
            compose.onNodeWithText("۰۹۱۲۱۲۳۴۵۶۷").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("۰۹۱۲۱۲۳۴۵۶۷").assertIsDisplayed()
            compose.onNodeWithText("تماس با این شماره").performClick()
            compose.onNodeWithText("تماس برقرار شود؟").assertIsDisplayed()
            compose.onNodeWithText("انصراف").performClick()
            assertTrue(intercepted.isEmpty())
            compose.onNodeWithText("تماس با این شماره").performClick()
            compose.onNodeWithText("شروع تماس").performClick()
            compose.waitUntil(5_000) { intercepted.isNotEmpty() }
            assertEquals(1, intercepted.size)
            assertEquals(Intent.ACTION_CALL, intercepted.single().action)
            assertEquals("tel:09121234567", intercepted.single().dataString)
            compose.onNodeWithTag("dial-key-delete").performScrollTo().performClick()
            compose.onNodeWithText("۰۹۱۲۱۲۳۴۵۶").assertIsDisplayed()
            compose.onNodeWithText("بازگشت به روشنو").performClick()
            compose.onNodeWithText("کمک می‌خواهم").assertIsDisplayed()
        } finally {
            scenario.close()
            instrumentation.removeMonitor(monitor)
            store.save(original)
        }
    }
}
