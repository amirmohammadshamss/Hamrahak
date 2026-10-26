package ir.hamrahak.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class PhoneContact(val id: Long, val name: String, val numbers: List<String>)

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
            result += PhoneContact(id, cursor.getString(1)?.takeIf { it.isNotBlank() } ?: "بدون نام",
                numbers[id].orEmpty().distinctBy { normalizeDialNumber(it) ?: it })
        }
    }
    return result
}

internal fun newPhoneContactIntent() = Intent(Intent.ACTION_INSERT).apply {
    type = ContactsContract.Contacts.CONTENT_TYPE
    putExtra("finishActivityOnSaveCompleted", true)
}

@Composable
internal fun PhoneContactsScreen(resumed: Int, size: Int, onHome: () -> Unit, onCall: (String) -> Unit, open: (Intent) -> Unit,
    loadContacts: (android.content.Context) -> List<PhoneContact> = ::readPhoneContacts) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var allowed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf(emptyList<PhoneContact>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var calling by rememberSaveable { mutableStateOf<String?>(null) }
    var waiting by rememberSaveable { mutableStateOf<String?>(null) }
    var callDenied by rememberSaveable { mutableStateOf(false) }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { revision++ }
    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = waiting; waiting = null; callDenied = !granted
        if (granted) calling = number
    }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { revision++ }
        }
        context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    LaunchedEffect(resumed, revision) {
        allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) { contacts = emptyList(); selectedId = null; loading = false; return@LaunchedEffect }
        loading = true; failed = false
        try { contacts = withContext(Dispatchers.IO) { loadContacts(context) } }
        catch (_: SecurityException) { allowed = false; contacts = emptyList() }
        catch (_: Exception) { failed = true; contacts = emptyList() }
        finally { loading = false }
    }
    fun requestCall(number: String) {
        val safe = normalizeDialNumber(number) ?: return
        callDenied = false
        if (usesEmergencyDialer(safe) || ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) calling = safe
        else { waiting = safe; callPermission.launch(Manifest.permission.CALL_PHONE) }
    }
    val textSize = (22 + size * 3).sp
    Column(Modifier.fillMaxSize().imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) { Text("بازگشت به همراهک", fontSize = 22.sp) }
        Text("مخاطبین", fontSize = textSize, fontWeight = FontWeight.Bold)
        Button(onClick = { open(newPhoneContactIntent()) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("افزودن مخاطب جدید", fontSize = 22.sp) }
        if (!allowed) {
            Text("برای دیدن همهٔ مخاطبین گوشی، اجازهٔ دسترسی به مخاطبین را بدهید.", fontSize = 22.sp)
            Button(onClick = { readPermission.launch(Manifest.permission.READ_CONTACTS) }, modifier = Modifier.fillMaxWidth()) { Text("نمایش مخاطبین گوشی", fontSize = 22.sp) }
            OutlinedButton(onClick = { open(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", context.packageName, null))) }, modifier = Modifier.fillMaxWidth()) { Text("تنظیم اجازه در گوشی", fontSize = 20.sp) }
        } else {
            OutlinedTextField(query, { query = it }, label = { Text("جستجوی نام یا شماره") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("contacts-search"))
            val filtered = contacts.filter { it.name.contains(query.trim(), ignoreCase = true) ||
                it.numbers.any { number -> comparableSmsAddress(number).contains(comparableSmsAddress(query.trim())) && query.isNotBlank() } }
            when {
                loading -> Text("در حال خواندن مخاطبین…", fontSize = 22.sp)
                failed -> { Text("خواندن مخاطبین ممکن نشد.", fontSize = 22.sp); Button(onClick = { revision++ }) { Text("تلاش دوباره") } }
                filtered.isEmpty() -> Text(if (contacts.isEmpty()) "هنوز مخاطبی در گوشی ذخیره نشده است." else "مخاطبی پیدا نشد.", fontSize = 22.sp)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { contact ->
                    OutlinedButton(onClick = {
                        if (contact.numbers.size == 1 && normalizeDialNumber(contact.numbers.single()) != null) requestCall(contact.numbers.single())
                        else selectedId = contact.id
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp).testTag("phone-contact-${contact.id}")) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(contact.name, fontSize = textSize, fontWeight = FontWeight.Bold)
                            Text(contact.numbers.firstOrNull() ?: "بدون شمارهٔ تلفن", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        if (callDenied) Text("اجازهٔ تماس داده نشد. دوباره مخاطب را بزنید یا اجازه را در تنظیمات گوشی فعال کنید.", fontSize = 20.sp)
    }
    contacts.find { it.id == selectedId }?.let { contact ->
        AlertDialog(onDismissRequest = { selectedId = null }, title = { Text(contact.name) }, text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (contact.numbers.isEmpty()) item { Text("این مخاطب شمارهٔ تلفن ندارد.", fontSize = 22.sp) }
                items(contact.numbers) { number ->
                    OutlinedButton(onClick = { selectedId = null; requestCall(number) }, enabled = normalizeDialNumber(number) != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(number, fontSize = 24.sp) }
                }
            }
        }, confirmButton = { TextButton(onClick = { selectedId = null }) { Text("بازگشت", fontSize = 22.sp) } })
    }
    calling?.let { number ->
        AlertDialog(onDismissRequest = { calling = null }, title = { Text("تماس برقرار شود؟", fontSize = 24.sp) },
            text = { Text(number, fontSize = 28.sp) },
            confirmButton = { TextButton(onClick = { calling = null; onCall(number) }) {
                Text(if (usesEmergencyDialer(number)) "باز کردن شماره‌گیر گوشی" else "شروع تماس", fontSize = 22.sp)
            } }, dismissButton = { TextButton(onClick = { calling = null }) { Text("انصراف", fontSize = 22.sp) } })
    }
}
