package ir.rosheno.launcher

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import android.content.ContentUris
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
    @get:Rule val permissions = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.CALL_PHONE)
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
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent {
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
        compose.onNodeWithText("مخاطب جدید").assertIsDisplayed()
        compose.onNodeWithText("ذخیرهٔ مخاطب").assertIsNotEnabled()
        compose.onNodeWithTag("new-contact-name").performTextInput("New Person")
        compose.onNodeWithTag("new-contact-number").performTextInput("*123#")
        compose.onNodeWithText("ذخیرهٔ مخاطب").assertIsNotEnabled()
        compose.onNodeWithTag("new-contact-number").performTextClearance()
        compose.onNodeWithTag("new-contact-number").performTextInput("۰۹۱۲۱۲۳۴۵۶۷")
        compose.onNodeWithText("ذخیرهٔ مخاطب").assertIsEnabled()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("new-contact-name").assertTextContains("New Person")
        compose.onNodeWithTag("new-contact-number").assertTextContains("۰۹۱۲۱۲۳۴۵۶۷")
        compose.onNodeWithText("انصراف و بازگشت").performScrollTo().performClick()
        assertTrue(opened.isEmpty())
    }

    @Test fun inAppFormSavesToDeviceContacts() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val marker = "Rosheno Test " + java.util.UUID.randomUUID().toString()
        try {
            compose.setContent {
                MaterialTheme { PhoneContactsScreen(0, 2, {}, {}, { error("Must stay inside app") }) }
            }
            compose.onNodeWithText("افزودن مخاطب جدید").performClick()
            compose.onNodeWithTag("new-contact-name").performTextInput(marker)
            compose.onNodeWithTag("new-contact-number").performTextInput("۰۹۱۲۱۲۳۴۵۶۷")
            compose.onNodeWithText("ذخیرهٔ مخاطب").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("مخاطب در گوشی ذخیره شد.").fetchSemanticsNodes().isNotEmpty()
            }
            val saved = readPhoneContacts(context).single { it.name == marker }
            assertEquals(listOf("09121234567"), saved.numbers)
            compose.onNodeWithTag("contacts-search").performTextInput(marker)
            compose.onNodeWithTag("phone-contact-${saved.id}").assertExists()
        } finally {
            // Remove only the synthetic record created by this test, even on assertion failure.
            val ids = mutableListOf<Long>()
            context.contentResolver.query(ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
                "${ContactsContract.Data.MIMETYPE}=? AND ${ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME}=?",
                arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, marker), null)?.use { cursor ->
                while (cursor.moveToNext()) ids += cursor.getLong(0)
            }
            ids.distinct().forEach { id -> context.contentResolver.delete(
                ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id), null, null) }
        }
    }
}
