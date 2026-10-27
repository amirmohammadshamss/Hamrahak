package ir.rosheno.launcher

import ir.rosheno.launcher.presentation.common.resolve
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.rosheno.launcher.platform.PlatformActions
import ir.rosheno.launcher.presentation.contacts.ContactsViewModel
import ir.rosheno.launcher.presentation.photos.CameraScreen
import ir.rosheno.launcher.presentation.sms.SmsScreen
import ir.rosheno.launcher.presentation.theme.RoshenoTheme
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

    @Test fun cameraHasPermissionExplanationWithoutReturnButton() {
        denied(Manifest.permission.CAMERA)
        var home = false
        compose.setContent { RoshenoTheme { CameraScreen({ home = true }, { error("No capture expected") }) } }
        compose.onNodeWithText("اجازهٔ استفاده از دوربین").assertExists()
        compose.onNodeWithTag("camera-shutter").assertDoesNotExist()
        compose.onNodeWithText("بازگشت به روشنو").assertDoesNotExist()
        assertFalse(home)
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
        lateinit var model: ContactsViewModel
        try {
            compose.runOnIdle {
                model = androidx.lifecycle.ViewModelProvider(owner,
                    object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                            ContactsViewModel(androidx.lifecycle.SavedStateHandle(),
                                ir.rosheno.launcher.di.AppContainer(compose.activity.application).phoneContacts) as T
                    })[ContactsViewModel::class.java]
                model.save("Denied test", "09121234567")
            }
            compose.waitUntil(5_000) { !model.state.value.saving && model.state.value.error != null }
            assertFalse(model.state.value.saved)
            assertTrue(requireNotNull(model.state.value.error).resolve(compose.activity).contains("اجازهٔ ذخیره"))
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
