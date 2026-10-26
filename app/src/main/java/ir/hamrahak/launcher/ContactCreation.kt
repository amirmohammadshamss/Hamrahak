package ir.hamrahak.launcher

import android.app.Application
import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A single provider transaction prevents a partially created name/phone record. */
internal fun createPhoneContact(context: Context, name: String, number: String): Uri {
    require(name.trim().isNotEmpty() && name.trim().length <= 100)
    val safe = requireNotNull(normalizePhone(number))
    val operations = arrayListOf(
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build(),
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.trim()).build(),
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, safe)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build(),
    )
    return requireNotNull(context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations).first().uri)
}

/** Saving survives activity recreation; repeated taps cannot start duplicate writes. */
internal class ContactCreationModel(application: Application) : AndroidViewModel(application) {
    var saving by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun reset() { if (!saving) { saved = false; error = null } }
    fun save(name: String, number: String) {
        if (saving || saved) return
        saving = true; error = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { createPhoneContact(getApplication(), name, number) }
                saved = true
            } catch (_: SecurityException) {
                error = "اجازهٔ ذخیرهٔ مخاطب فعال نیست. دسترسی مخاطبین را در تنظیمات گوشی بررسی کنید."
            } catch (_: Exception) {
                error = "مخاطب ذخیره نشد. نام و شماره حفظ شده است؛ دوباره تلاش کنید."
            } finally { saving = false }
        }
    }
}
