package ir.hamrahak.launcher

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat

@Composable
fun DialerScreen(number: String, onNumberChange: (String) -> Unit, contacts: List<FamilyContact>, size: Int,
    onHome: () -> Unit, onCall: (String) -> Unit, onSystemDial: (String) -> Unit) {
    val context = LocalContext.current
    var confirming by rememberSaveable { mutableStateOf<String?>(null) }
    var requestingNumber by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var chooseFamily by rememberSaveable { mutableStateOf(false) }
    val textSize = 22 + size * 3
    val valid = normalizeDialNumber(number)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val requested = requestingNumber
        requestingNumber = null
        permissionDenied = !granted
        // A permission grant only opens confirmation. It never starts a call.
        if (granted && requested != null) confirming = requested
    }
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("بازگشت به همراهک", fontSize = 22.sp)
        }
        Text("شماره‌گیر", fontSize = textSize.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(number, { onNumberChange(it.take(32)) }, label = { Text("شمارهٔ تلفن") },
            modifier = Modifier.fillMaxWidth().testTag("dialer-number"), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, showKeyboardOnFocus = false),
            textStyle = LocalTextStyle.current.copy(fontSize = (28 + size * 2).sp,
                textAlign = TextAlign.Center, textDirection = TextDirection.Ltr),
            isError = number.isNotEmpty() && valid == null)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (permissionDenied) {
                Text("اجازهٔ تماس داده نشد. شمارهٔ شما حفظ شده است؛ می‌توانید از شماره‌گیر گوشی ادامه دهید.", fontSize = 20.sp, color = Muted)
                OutlinedButton(onClick = { valid?.let(onSystemDial) }, enabled = valid != null, modifier = Modifier.fillMaxWidth()) {
                    Text("ادامه با شماره‌گیر گوشی", fontSize = 20.sp)
                }
            }
            if (contacts.isNotEmpty()) OutlinedButton(onClick = { chooseFamily = true }, modifier = Modifier.fillMaxWidth()) {
                Text("انتخاب شماره از خانواده", fontSize = 20.sp)
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
                            Text(if (key == "delete") "پاک" else persianDigits(key), fontSize = if (key == "delete") 22.sp else 30.sp,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                } }
            }
            TextButton(onClick = { onNumberChange("") }, enabled = number.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("پاک کردن همهٔ شماره", fontSize = 18.sp)
            }
        }
        Button(enabled = valid != null, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), onClick = {
            valid?.let { safe ->
                if (usesEmergencyDialer(safe) || ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    confirming = safe
                } else { requestingNumber = safe; permission.launch(Manifest.permission.CALL_PHONE) }
            }
        }) { Text("تماس با این شماره", fontSize = textSize.sp) }
    }
    confirming?.let { snapshot ->
        val emergency = usesEmergencyDialer(snapshot)
        val name = contacts.find { comparableSmsAddress(it.number) == comparableSmsAddress(snapshot) }?.name
        AlertDialog(onDismissRequest = { confirming = null }, title = { Text("تماس برقرار شود؟", fontSize = 24.sp) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                name?.let { Text(it, fontSize = textSize.sp, fontWeight = FontWeight.Bold) }
                Text(snapshot, fontSize = (textSize + 4).sp, modifier = Modifier.fillMaxWidth(),
                    style = LocalTextStyle.current.copy(textAlign = TextAlign.Center, textDirection = TextDirection.Ltr))
                Text(if (emergency) "این شماره از طریق شماره‌گیر اضطراری گوشی باز می‌شود."
                    else "تماس از طریق سیم‌کارت گوشی شروع می‌شود. هزینه طبق تعرفهٔ سیم‌کارت است.", fontSize = 20.sp)
            } },
            confirmButton = { TextButton(onClick = {
                val accepted = confirming
                confirming = null
                if (accepted != null) onCall(accepted)
            }) { Text(if (emergency) "باز کردن شماره‌گیر گوشی" else "شروع تماس", fontSize = 22.sp) } },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("انصراف", fontSize = 22.sp) } })
    }
    if (chooseFamily) AlertDialog(onDismissRequest = { chooseFamily = false }, title = { Text("انتخاب شماره") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            contacts.forEach { contact -> OutlinedButton(onClick = { onNumberChange(contact.number); chooseFamily = false }, modifier = Modifier.fillMaxWidth()) {
                Text(contact.name, fontSize = 22.sp)
            } }
        } }, confirmButton = { TextButton(onClick = { chooseFamily = false }) { Text("بازگشت") } })
}
