package ir.hamrahak.launcher

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SmsScreen(contacts: List<FamilyContact>, size: Int, onHome: () -> Unit, openSystemMessages: () -> Unit,
    vm: SmsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var mode by rememberSaveable { mutableStateOf("inbox") }
    var threadId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmationAddress by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmationBody by rememberSaveable { mutableStateOf("") }
    var choosingContact by rememberSaveable { mutableStateOf(false) }
    var replaceDraftFor by rememberSaveable { mutableStateOf<String?>(null) }
    var clearingDraft by rememberSaveable { mutableStateOf(false) }
    val textSize = 22 + size * 3
    val waiting = state.submitting || state.report?.let { !it.complete && !state.reportExpired } == true
    fun confirmDraft() {
        if (normalizePhone(state.recipient) == null || state.draft.isBlank()) {
            vm.error("شمارهٔ گیرنده و متن پیام را وارد کنید.")
        } else { confirmationAddress = state.recipient; confirmationBody = state.draft }
    }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) vm.start() else vm.error("برای دیدن پیامک‌ها، اجازهٔ خواندن لازم است. می‌توانید از برنامهٔ پیامک گوشی استفاده کنید.")
    }
    val sendPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) confirmDraft() else vm.error("اجازهٔ ارسال داده نشد. متن پیام در پیش‌نویس مانده است.")
    }
    DisposableEffect(vm, owner) {
        val listener = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.start()
                Lifecycle.Event.ON_STOP -> vm.stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(listener)
        onDispose { owner.lifecycle.removeObserver(listener); vm.stop() }
    }
    BackHandler(mode != "inbox") { mode = "inbox" }
    val threads = remember(state.messages) { messageThreads(state.messages) }
    val thread = threads.find { it.id == threadId }
    fun sender(address: String): String = contacts.find {
        comparableSmsAddress(it.number) == comparableSmsAddress(address)
    }?.name ?: address.ifBlank { "شمارهٔ نامشخص" }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("بازگشت به همراهک", fontSize = 22.sp)
        }
        Text(if (mode == "compose") "نوشتن پیامک" else if (mode == "thread") thread?.let { sender(it.address) } ?: "گفت‌وگو" else "پیامک‌ها",
            fontSize = textSize.sp, fontWeight = FontWeight.Bold)
        state.error?.let { error -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(error, fontSize = 18.sp)
                TextButton(onClick = { vm.error(null) }) { Text("متوجه شدم") }
            }
        } }
        state.report?.let { report -> Text(smsReportLabel(report), fontSize = 18.sp, color = if (report.failed) MaterialTheme.colorScheme.error else Green,
            modifier = Modifier.testTag("sms-send-status")) }
        if (mode == "compose") {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(state.recipient, { vm.draft(recipient = it) }, label = { Text("شمارهٔ گیرنده") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !waiting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = LocalTextStyle.current.copy(fontSize = textSize.sp, textDirection = TextDirection.Ltr))
                if (contacts.isNotEmpty()) OutlinedButton(onClick = { choosingContact = true }, enabled = !waiting) { Text("انتخاب از خانواده", fontSize = 20.sp) }
                OutlinedTextField(state.draft, { vm.draft(text = it) }, label = { Text("متن پیام") }, minLines = 4,
                    modifier = Modifier.fillMaxWidth(), enabled = !waiting,
                    textStyle = LocalTextStyle.current.copy(fontSize = textSize.sp))
                Text("${persianDigits(smsPartCount(state.draft).toString())} بخش پیامک · هزینه طبق تعرفهٔ سیم‌کارت", fontSize = 16.sp, color = Muted)
                Button(enabled = !waiting && normalizePhone(state.recipient) != null && state.draft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), onClick = {
                        if (vm.hasPermission(Manifest.permission.SEND_SMS)) confirmDraft()
                        else sendPermission.launch(Manifest.permission.SEND_SMS)
                    }) { Text(if (waiting) "در حال فرستادن…" else "بررسی و ارسال", fontSize = 22.sp) }
                OutlinedButton(onClick = { mode = "inbox" }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("همهٔ گفت‌وگوها", fontSize = 20.sp) }
                if (state.draft.isNotBlank()) TextButton(onClick = { clearingDraft = true }, enabled = !waiting) { Text("پاک کردن پیش‌نویس", fontSize = 18.sp) }
            }
        } else {
            Button(onClick = {
                val address = if (mode == "thread") thread?.address else null
                if (address != null && state.draft.isNotBlank() && comparableSmsAddress(address) != comparableSmsAddress(state.recipient)) replaceDraftFor = address
                else { if (address != null) vm.draft(recipient = address); mode = "compose" }
            }, enabled = !waiting && (mode != "thread" || normalizePhone(thread?.address.orEmpty()) != null),
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text(if (mode == "thread") "پاسخ دادن" else if (state.draft.isNotBlank()) "ادامهٔ پیش‌نویس" else "نوشتن پیام جدید", fontSize = 22.sp)
            }
            if (!state.canRead) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LauncherIcon("message", Green)
                    Text("برای دیدن فهرست و متن پیامک‌های همین گوشی، اجازهٔ خواندن پیامک لازم است. اطلاعات به سرور فرستاده نمی‌شود.", fontSize = 22.sp)
                    Button(onClick = { readPermission.launch(Manifest.permission.READ_SMS) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                        Text("اجازهٔ خواندن پیامک‌ها", fontSize = 20.sp)
                    }
                    OutlinedButton(onClick = openSystemMessages, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("باز کردن پیامک‌های گوشی", fontSize = 20.sp) }
                    TextButton(onClick = {
                        runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null))) }
                            .onFailure { vm.error("از تنظیمات گوشی، اجازهٔ پیامک همراهک را بررسی کنید.") }
                    }) { Text("تنظیم اجازهٔ پیامک", fontSize = 18.sp) }
                }
            } else {
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mode == "thread") {
                        item { OutlinedButton(onClick = { mode = "inbox" }) { Text("همهٔ گفت‌وگوها", fontSize = 20.sp) } }
                        if (thread == null && !state.loading) item { Text("پیامکی برای نمایش پیدا نشد.", fontSize = 22.sp) }
                        items(thread?.messages?.sortedBy { it.date }.orEmpty(), key = { it.id }) { message ->
                            Surface(shape = RoundedCornerShape(20.dp), color = if (message.type == TelephonyType.INBOX) Pale else MaterialTheme.colorScheme.surface) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(smsTypeLabel(message.type), fontSize = 16.sp, color = Muted)
                                    SelectionContainer { Text(message.body.ifEmpty { "پیامک بدون متن" }, fontSize = textSize.sp) }
                                    Text(smsDate(message.date), fontSize = 16.sp, color = Muted)
                                }
                            }
                        }
                    } else {
                        if (threads.isEmpty() && !state.loading) item { Text("هنوز پیامکی برای نمایش وجود ندارد.", fontSize = 22.sp) }
                        items(threads, key = { it.id }) { conversation ->
                            Surface(onClick = { threadId = conversation.id; mode = "thread" }, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Hairline)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(sender(conversation.address), fontSize = textSize.sp, fontWeight = FontWeight.Bold)
                                    Text(conversation.messages.first().body, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(smsDate(conversation.messages.first().date), fontSize = 16.sp, color = Muted)
                                }
                            }
                        }
                    }
                    if (state.hasMore) item { OutlinedButton(onClick = vm::more, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text("نمایش پیامک‌های قدیمی‌تر", fontSize = 20.sp) } }
                    if (state.error != null) item { OutlinedButton(onClick = openSystemMessages, modifier = Modifier.fillMaxWidth()) { Text("باز کردن پیامک‌های گوشی", fontSize = 20.sp) } }
                }
            }
        }
    }
    confirmationAddress?.let { address -> AlertDialog(onDismissRequest = { confirmationAddress = null },
        title = { Text("این پیامک فرستاده شود؟", fontSize = 24.sp) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(address, style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr), fontSize = 22.sp)
            Text(confirmationBody, fontSize = 22.sp)
            Text("${persianDigits(smsPartCount(confirmationBody).toString())} بخش پیامک؛ هزینه طبق تعرفهٔ سیم‌کارت.", fontSize = 18.sp)
        } },
        confirmButton = { TextButton(onClick = { confirmationAddress = null; vm.sendConfirmed(address, confirmationBody) }) { Text("ارسال پیامک", fontSize = 22.sp) } },
        dismissButton = { TextButton(onClick = { confirmationAddress = null }) { Text("انصراف", fontSize = 22.sp) } }) }
    if (choosingContact) AlertDialog(onDismissRequest = { choosingContact = false }, title = { Text("انتخاب گیرنده") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            contacts.forEach { contact -> OutlinedButton(onClick = { vm.draft(recipient = contact.number); choosingContact = false }, modifier = Modifier.fillMaxWidth()) { Text(contact.name, fontSize = 22.sp) } }
        } }, confirmButton = { TextButton(onClick = { choosingContact = false }) { Text("بازگشت") } })
    replaceDraftFor?.let { address -> AlertDialog(onDismissRequest = { replaceDraftFor = null }, title = { Text("پیش‌نویس قبلی وجود دارد") },
        text = { Text("می‌توانید پیش‌نویس قبلی را ادامه دهید یا متن آن را پاک کنید و برای این شماره پیام تازه بنویسید.", fontSize = 20.sp) },
        confirmButton = { TextButton(onClick = { replaceDraftFor = null; vm.draft(recipient = address, text = ""); mode = "compose" }) { Text("پیام تازه برای این شماره") } },
        dismissButton = { TextButton(onClick = { replaceDraftFor = null; mode = "compose" }) { Text("ادامهٔ پیش‌نویس قبلی") } }) }
    if (clearingDraft) AlertDialog(onDismissRequest = { clearingDraft = false }, title = { Text("پیش‌نویس پاک شود؟") },
        confirmButton = { TextButton(onClick = { vm.draft(text = "", recipient = ""); clearingDraft = false }) { Text("پاک کردن") } },
        dismissButton = { TextButton(onClick = { clearingDraft = false }) { Text("انصراف") } })
}

private object TelephonyType { const val INBOX = 1 }
fun comparableSmsAddress(address: String): String {
    val normalized = normalizePhone(address) ?: return address.trim()
    return when {
        normalized.startsWith("+98") -> "0" + normalized.drop(3)
        normalized.startsWith("0098") -> "0" + normalized.drop(4)
        else -> normalized
    }
}
private fun smsDate(date: Long): String = android.icu.text.DateFormat.getInstanceForSkeleton("MMMdHm",
    android.icu.util.ULocale("fa_IR@calendar=persian")).format(java.util.Date(date))
private fun smsTypeLabel(type: Int): String = when (type) {
    1 -> "دریافتی"; 2 -> "ارسالی"; 3 -> "پیش‌نویس"; 4, 6 -> "در انتظار ارسال"; 5 -> "ارسال ناموفق"; else -> "پیامک"
}
fun smsReportLabel(report: SmsSendReport): String = when {
    report.failed && report.results.any { it == android.app.Activity.RESULT_OK } -> "بخشی از پیامک ارسال نشد؛ قبل از ارسال دوباره وضعیت را بررسی کنید."
    report.failed -> "ارسال پیامک ناموفق بود."
    report.sent -> "پیامک ارسال شد."
    report.expired -> "وضعیت ارسال مشخص نیست؛ پیامک‌های گوشی را بررسی کنید."
    else -> "پیامک در حال ارسال است…"
}
