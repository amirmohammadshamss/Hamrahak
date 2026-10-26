package ir.rosheno.launcher

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run in the denied-permission phase before any GrantPermissionRule tests. */
@RunWith(AndroidJUnit4::class)
class NoPermissionFeaturesTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun denied(permission: String) = assertEquals(PackageManager.PERMISSION_DENIED,
        ContextCompat.checkSelfPermission(compose.activity, permission))

    @Test fun cameraHasPermissionExplanationAndHomeEscape() {
        denied(Manifest.permission.CAMERA)
        var home = false
        compose.setContent { RoshenoTheme { CameraScreen({ home = true }, { error("No capture expected") }) } }
        compose.onNodeWithText("اجازهٔ استفاده از دوربین").assertExists()
        compose.onNodeWithTag("camera-shutter").assertDoesNotExist()
        compose.onNodeWithText("بازگشت به روشنو").performClick()
        assertTrue(home)
    }
    @Test fun smsHasReadPermissionExplanationAndSystemFallback() {
        denied(Manifest.permission.READ_SMS)
        denied(Manifest.permission.SEND_SMS)
        var fallback = false
        compose.setContent { RoshenoTheme { SmsScreen(emptyList(), 0, {}, { fallback = true }) } }
        compose.onNodeWithText("اجازهٔ خواندن پیامک‌ها").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("باز کردن پیامک‌های گوشی").performScrollTo().performClick()
        assertTrue(fallback)
    }
    @Test fun deniedContactWriteReportsFailureWithoutSaving() {
        denied(Manifest.permission.WRITE_CONTACTS)
        val owner = androidx.lifecycle.ViewModelStore()
        lateinit var model: ContactCreationModel
        try {
            compose.runOnIdle {
                model = androidx.lifecycle.ViewModelProvider(owner,
                    androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(compose.activity.application))[ContactCreationModel::class.java]
                model.save("Denied test", "09121234567")
            }
            compose.waitUntil(5_000) { !model.saving && model.error != null }
            assertFalse(model.saved)
            assertTrue(requireNotNull(model.error).contains("اجازهٔ ذخیره"))
        } finally { compose.runOnIdle { owner.clear() } }
    }
    @Test fun directCallCannotBypassDeniedPermission() {
        denied(Manifest.permission.CALL_PHONE)
        var message: String? = null
        compose.runOnIdle {
            PlatformActions(compose.activity, { message = it }, {}).startUserCall("09121234567")
        }
        assertNotNull(message)
        assertTrue(requireNotNull(message).contains("اجازهٔ تماس"))
    }
}
