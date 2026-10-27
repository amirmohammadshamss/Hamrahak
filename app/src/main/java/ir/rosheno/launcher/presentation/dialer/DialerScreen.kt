package ir.rosheno.launcher.presentation.dialer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.phone.comparableSmsAddress
import ir.rosheno.launcher.domain.phone.persianDigits
import ir.rosheno.launcher.platform.normalizeDialNumber
import ir.rosheno.launcher.platform.usesEmergencyDialer
import ir.rosheno.launcher.presentation.theme.Muted

@Composable
fun DialerScreen(number: String, onNumberChange: (String) -> Unit, contacts: List<FamilyContact>, size: Int,
    onHome: () -> Unit, onCall: (String) -> Unit, onSystemDial: (String) -> Unit,
    model: DialerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    val textSize = 22 + size * 3
    val valid = normalizeDialNumber(number)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), model::permissionResult)
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(context.getString(R.string.dialer_title), fontSize = textSize.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(number, { onNumberChange(it.take(32)) }, label = { Text(context.getString(R.string.phone_number_label)) },
            modifier = Modifier.fillMaxWidth().testTag("dialer-number"), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, showKeyboardOnFocus = false),
            textStyle = LocalTextStyle.current.copy(fontSize = (28 + size * 2).sp,
                textAlign = TextAlign.Center, textDirection = TextDirection.Ltr),
            isError = number.isNotEmpty() && valid == null)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.permissionDenied) {
                Text(context.getString(R.string.dialer_permission_denied), fontSize = 20.sp, color = Muted)
                OutlinedButton(onClick = { valid?.let(onSystemDial) }, enabled = valid != null, modifier = Modifier.fillMaxWidth()) {
                    Text(context.getString(R.string.action_continue_system_dialer), fontSize = 20.sp)
                }
            }
            if (contacts.isNotEmpty()) OutlinedButton(onClick = { model.chooseFamily(true) }, modifier = Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.action_choose_family_number), fontSize = 20.sp)
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("+", "0", "delete"))
                .forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        val enabled = when (key) { "+" -> number.isEmpty(); "delete" -> number.isNotEmpty(); else -> number.length < 32 }
                        OutlinedButton(onClick = {
                            onNumberChange(when (key) {
                                "delete" -> number.dropLast(1)
                                "+" -> "+"
                                else -> number + persianDigits(key)
                            })
                        }, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 80.dp).testTag("dial-key-$key"),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                            Text(if (key == "delete") context.getString(R.string.dialer_key_delete) else persianDigits(key), fontSize = if (key == "delete") 22.sp else 30.sp,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                } }
            }
            TextButton(onClick = { onNumberChange("") }, enabled = number.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.action_clear_number), fontSize = 18.sp)
            }
        }
        Button(enabled = valid != null, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), onClick = {
            val canCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            if (model.requestCall(number, canCall)) permission.launch(Manifest.permission.CALL_PHONE)
        }) { Text(context.getString(R.string.action_call_number), fontSize = textSize.sp) }
    }
    state.confirming?.let { snapshot ->
        val emergency = usesEmergencyDialer(snapshot)
        val name = contacts.find { comparableSmsAddress(it.number) == comparableSmsAddress(snapshot) }?.name
        AlertDialog(onDismissRequest = { model.dismissConfirmation() }, title = { Text(context.getString(R.string.call_confirmation_title), fontSize = 24.sp) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                name?.let { Text(it, fontSize = textSize.sp, fontWeight = FontWeight.Bold) }
                Text(snapshot, fontSize = (textSize + 4).sp, modifier = Modifier.fillMaxWidth(),
                    style = LocalTextStyle.current.copy(textAlign = TextAlign.Center, textDirection = TextDirection.Ltr))
                Text(if (emergency) context.getString(R.string.call_emergency_description)
                    else context.getString(R.string.call_cost_description), fontSize = 20.sp)
            } },
            confirmButton = { TextButton(onClick = {
                model.consumeConfirmation()?.let(onCall)
            }) { Text(if (emergency) context.getString(R.string.action_open_system_dialer) else context.getString(R.string.action_start_call), fontSize = 22.sp) } },
            dismissButton = { TextButton(onClick = { model.dismissConfirmation() }) { Text(context.getString(R.string.action_cancel), fontSize = 22.sp) } })
    }
    if (state.chooseFamily) AlertDialog(onDismissRequest = { model.chooseFamily(false) }, title = { Text(context.getString(R.string.choose_number_title)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            contacts.forEach { contact -> OutlinedButton(onClick = { onNumberChange(contact.number); model.chooseFamily(false) }, modifier = Modifier.fillMaxWidth()) {
                Text(contact.name, fontSize = 22.sp)
            } }
        } }, confirmButton = { TextButton(onClick = { model.chooseFamily(false) }) { Text(context.getString(R.string.action_back)) } })
}
