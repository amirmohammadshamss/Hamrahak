package ir.hamrahak.launcher

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneContactsFeatureTest {
    @get:Rule val permissions = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
    @get:Rule val compose = createComposeRule()

    @Test fun deviceContactsProviderCanBeRead() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val contacts = readPhoneContacts(context)
        assertEquals(contacts.size, contacts.map { it.id }.distinct().size)
        assertTrue(contacts.all { it.name.isNotBlank() })
    }

    @Test fun searchMultipleNumbersConfirmCancelAndCreateContact() {
        val calls = mutableListOf<String>()
        val opened = mutableListOf<Intent>()
        compose.setContent {
            MaterialTheme {
                PhoneContactsScreen(0, 2, {}, { calls += it }, { opened += it }, loadContacts = {
                    listOf(PhoneContact(1, "Test Alice", listOf("09121234567", "02112345678")),
                        PhoneContact(2, "Test Bob", emptyList()))
                })
            }
        }
        compose.onNodeWithText("Test Alice").assertExists()
        compose.onNodeWithTag("phone-contact-2").performClick()
        compose.onNodeWithText("این مخاطب شمارهٔ تلفن ندارد.").assertExists()
        compose.onNodeWithText("بازگشت").performClick()
        compose.onNodeWithTag("contacts-search").performTextInput("Alice")
        compose.onNodeWithText("Test Bob").assertDoesNotExist()
        compose.onNodeWithTag("phone-contact-1").performClick()
        compose.onNodeWithText("02112345678").performClick()
        compose.onNodeWithText("تماس برقرار شود؟").assertExists()
        compose.onNodeWithText("انصراف").performClick()
        assertTrue(calls.isEmpty())
        compose.onNodeWithTag("phone-contact-1").performClick()
        compose.onNodeWithText("02112345678").performClick()
        compose.onNodeWithText("شروع تماس").performClick()
        assertEquals(listOf("02112345678"), calls)
        compose.onNodeWithText("افزودن مخاطب جدید").performClick()
        assertEquals(Intent.ACTION_INSERT, opened.single().action)
        assertEquals(ContactsContract.Contacts.CONTENT_TYPE, opened.single().type)
    }
}
