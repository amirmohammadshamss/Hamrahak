package ir.rosheno.launcher

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.rosheno.launcher.platform.PlatformActions
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformFeatureTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun externalAppDialerFallbacksAndHomeSettingsUseExpectedIntents() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intents = CopyOnWriteArrayList<Intent>()
        val errors = mutableListOf<String>()
        val roles = mutableListOf<Intent>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult {
                intents += intent
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            compose.runOnIdle {
                val platform = PlatformActions(compose.activity, { errors += it }, { roles += it })
                platform.dial("۰۹۱۲۱۲۳۴۵۶۷")
                platform.launch("com.android.settings/.Settings")
                platform.messages(); platform.photos(); platform.camera(); platform.homeSettings()
                platform.dial("*123#"); platform.launch("invalid component")
                platform.chooseHome()
            }
            assertEquals(if (android.os.Build.VERSION.SDK_INT >= 29) 6 else 7, intents.size)
            assertEquals(Intent.ACTION_DIAL, intents[0].action)
            assertEquals("tel:09121234567", intents[0].dataString)
            assertEquals("com.android.settings", intents[1].component?.packageName)
            assertTrue(Intent.CATEGORY_APP_MESSAGING in intents[2].categories)
            assertTrue(Intent.CATEGORY_APP_GALLERY in intents[3].categories)
            assertEquals("android.media.action.STILL_IMAGE_CAMERA", intents[4].action)
            assertEquals(Settings.ACTION_HOME_SETTINGS, intents[5].action)
            assertTrue(errors.size >= 2)
            // Home-role requests are captured rather than granted by this test.
            if (android.os.Build.VERSION.SDK_INT >= 29)
                assertTrue(roles.isNotEmpty() || errors.any { it.contains("صفحهٔ اصلی") })
            else assertEquals(Settings.ACTION_HOME_SETTINGS, intents.last().action)
        } finally { instrumentation.removeMonitor(monitor) }
    }
}
