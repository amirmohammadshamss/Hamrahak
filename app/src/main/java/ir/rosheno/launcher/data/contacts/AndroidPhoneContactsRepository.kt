package ir.rosheno.launcher.data.contacts

import android.content.ContentProviderOperation
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.model.PhoneContact
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.repository.PhoneContactsRepository
import ir.rosheno.launcher.platform.normalizeDialNumber

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

internal fun readPhoneContacts(context: android.content.Context): List<PhoneContact> {
    val numbers = mutableMapOf<Long, MutableList<String>>()
    context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val number = cursor.getString(1) ?: continue
            numbers.getOrPut(cursor.getLong(0)) { mutableListOf() }.add(number)
        }
    }
    val result = mutableListOf<PhoneContact>()
    context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null,
        "${ContactsContract.Contacts.SORT_KEY_PRIMARY} ASC")?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            result += PhoneContact(id, cursor.getString(1)?.takeIf { it.isNotBlank() } ?: context.getString(R.string.contact_unnamed),
                numbers[id].orEmpty().distinctBy { normalizeDialNumber(it) ?: it })
        }
    }
    return result
}

class AndroidPhoneContactsRepository(private val context: Context) : PhoneContactsRepository {
    override fun load(): List<PhoneContact> = readPhoneContacts(context)
    override fun create(name: String, number: String) { createPhoneContact(context, name, number) }
    override fun observe(onChange: () -> Unit): () -> Unit {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { onChange() }
        }
        context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        return { context.contentResolver.unregisterContentObserver(observer) }
    }

}
