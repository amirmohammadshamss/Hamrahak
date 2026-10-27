package ir.rosheno.launcher.presentation.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.di.FeatureViewModelFactory
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.platform.normalizeDialNumber
import ir.rosheno.launcher.platform.usesEmergencyDialer
import ir.rosheno.launcher.presentation.common.resolve
import ir.rosheno.launcher.presentation.components.ReturnHomeButton

@Composable
internal fun PhoneContactsScreen(resumed: Int, size: Int, onHome: () -> Unit, onCall: (String) -> Unit, open: (Intent) -> Unit,
    model: ContactsViewModel = viewModel(factory = FeatureViewModelFactory)) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    fun canRead() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val writePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        model.writePermissionResult(grants[Manifest.permission.WRITE_CONTACTS] == true)
        model.start(canRead())
    }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.start(it) }
    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), model::callPermissionResult)
    DisposableEffect(model, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.start(canRead())
            else if (event == Lifecycle.Event.ON_STOP) model.stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); model.stop() }
    }
    LaunchedEffect(resumed) { model.start(canRead()) }
    fun requestCall(number: String) {
        val canCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        if (model.requestCall(number, canCall)) callPermission.launch(Manifest.permission.CALL_PHONE)
    }
    val textSize = (22 + size * 3).sp
    BackHandler(enabled = state.adding, onBack = model::cancelAdd)
    if (state.adding) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(context.getString(R.string.contact_new_title), fontSize = textSize, fontWeight = FontWeight.Bold)
            Text(context.getString(R.string.contact_create_description), fontSize = 20.sp)
            OutlinedTextField(state.draftName, model::editName, label = { Text(context.getString(R.string.contact_name_label)) },
                enabled = !state.saving, singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = textSize),
                modifier = Modifier.fillMaxWidth().testTag("new-contact-name"))
            OutlinedTextField(state.draftNumber, model::editNumber, label = { Text(context.getString(R.string.phone_number_label)) },
                enabled = !state.saving, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = LocalTextStyle.current.copy(fontSize = textSize, textDirection = TextDirection.Ltr),
                isError = state.draftNumber.isNotBlank() && normalizePhone(state.draftNumber) == null,
                modifier = Modifier.fillMaxWidth().testTag("new-contact-number"))
            state.saveNotice?.let { Text(it.resolve(context), fontSize = 20.sp) }
            state.error?.let { Text(it.resolve(context), fontSize = 20.sp, color = MaterialTheme.colorScheme.error) }
            Button(enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED)
                        model.save()
                    else writePermission.launch(arrayOf(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS))
                }) { Text(if (state.saving) context.getString(R.string.status_saving) else context.getString(R.string.action_save_contact), fontSize = textSize) }
            OutlinedButton(enabled = !state.saving, onClick = model::cancelAdd,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(context.getString(R.string.action_cancel_and_back), fontSize = 22.sp) }
            Text(context.getString(R.string.contact_write_permission_hint), fontSize = 18.sp)
            TextButton(onClick = { open(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", context.packageName, null))) }, enabled = !state.saving) { Text(context.getString(R.string.action_permission_settings), fontSize = 20.sp) }
        }
        return
    }
    Column(Modifier.fillMaxSize().imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReturnHomeButton(onHome, outlined = true, minHeight = 60.dp)
        Text(context.getString(R.string.contacts_title), fontSize = textSize, fontWeight = FontWeight.Bold)
        Button(onClick = model::add, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(context.getString(R.string.action_add_phone_contact), fontSize = 22.sp) }
        state.saveNotice?.let { Text(it.resolve(context), fontSize = 20.sp) }
        if (!state.allowed) {
            Text(context.getString(R.string.contacts_permission_description), fontSize = 22.sp)
            Button(onClick = { readPermission.launch(Manifest.permission.READ_CONTACTS) }, modifier = Modifier.fillMaxWidth()) { Text(context.getString(R.string.action_show_phone_contacts), fontSize = 22.sp) }
            OutlinedButton(onClick = { open(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", context.packageName, null))) }, modifier = Modifier.fillMaxWidth()) { Text(context.getString(R.string.action_permission_settings), fontSize = 20.sp) }
        } else {
            OutlinedTextField(state.query, model::search, label = { Text(context.getString(R.string.contacts_search)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("contacts-search"))
            val filtered = state.filtered
            when {
                state.loading -> Text(context.getString(R.string.contacts_loading), fontSize = 22.sp)
                state.failed -> { Text(context.getString(R.string.contacts_load_failed), fontSize = 22.sp); Button(onClick = { model.start(canRead()) }) { Text(context.getString(R.string.action_retry)) } }
                filtered.isEmpty() -> Text(if (state.contacts.isEmpty()) context.getString(R.string.contacts_empty) else context.getString(R.string.contacts_search_empty), fontSize = 22.sp)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { contact ->
                    OutlinedButton(onClick = {
                        if (contact.numbers.size == 1 && normalizeDialNumber(contact.numbers.single()) != null) requestCall(contact.numbers.single())
                        else model.select(contact.id)
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp).testTag("phone-contact-${contact.id}")) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(contact.name, fontSize = textSize, fontWeight = FontWeight.Bold)
                            Text(contact.numbers.firstOrNull() ?: context.getString(R.string.contact_no_phone), fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        if (state.callDenied) Text(context.getString(R.string.contacts_call_permission_denied), fontSize = 20.sp)
    }
    state.contacts.find { it.id == state.selectedId }?.let { contact ->
        AlertDialog(onDismissRequest = { model.select(null) }, title = { Text(contact.name) }, text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (contact.numbers.isEmpty()) item { Text(context.getString(R.string.contact_no_phone_description), fontSize = 22.sp) }
                items(contact.numbers) { number ->
                    OutlinedButton(onClick = { requestCall(number) }, enabled = normalizeDialNumber(number) != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(number, fontSize = 24.sp) }
                }
            }
        }, confirmButton = { TextButton(onClick = { model.select(null) }) { Text(context.getString(R.string.action_back), fontSize = 22.sp) } })
    }
    state.calling?.let { number ->
        AlertDialog(onDismissRequest = { model.dismissCall() }, title = { Text(context.getString(R.string.call_confirmation_title), fontSize = 24.sp) },
            text = { Text(number, fontSize = 28.sp) },
            confirmButton = { TextButton(onClick = { model.consumeCall()?.let(onCall) }) {
                Text(if (usesEmergencyDialer(number)) context.getString(R.string.action_open_system_dialer) else context.getString(R.string.action_start_call), fontSize = 22.sp)
            } }, dismissButton = { TextButton(onClick = { model.dismissCall() }) { Text(context.getString(R.string.action_cancel), fontSize = 22.sp) } })
    }
}
