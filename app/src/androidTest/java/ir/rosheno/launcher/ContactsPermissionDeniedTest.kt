package ir.rosheno.launcher

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.rosheno.launcher.presentation.contacts.PhoneContactsScreen
import ir.rosheno.launcher.presentation.contacts.ContactsViewModel
import ir.rosheno.launcher.domain.model.PhoneContact
import ir.rosheno.launcher.domain.repository.PhoneContactsRepository
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run with READ_CONTACTS revoked; intentionally has no GrantPermissionRule. */
@RunWith(AndroidJUnit4::class)
class ContactsPermissionDeniedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openingContactsWithoutPermissionShowsExplanationAndDoesNotReadProvider() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(PackageManager.PERMISSION_DENIED, ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS))
        assertEquals(PackageManager.PERMISSION_DENIED, ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS))
        var loaded = false
        var returnedHome = false
        val model = ContactsViewModel(SavedStateHandle(), object : PhoneContactsRepository {
            override fun load(): List<PhoneContact> {
                loaded = true
                error("Contacts must not be read without permission")
            }
            override fun create(name: String, number: String) { error("No write expected") }
        })
        compose.setContent {
            MaterialTheme { PhoneContactsScreen(0, 2, { returnedHome = true }, {}, {}, model = model) }
        }
        compose.onNodeWithText("نمایش مخاطبین گوشی").assertIsDisplayed()
        compose.onNodeWithText("تنظیم اجازه در گوشی").assertIsDisplayed()
        compose.onNodeWithTag("contacts-search").assertDoesNotExist()
        assertFalse(loaded)
        compose.onNodeWithText("بازگشت به روشنو").performClick()
        assertTrue(returnedHome)
    }
}
