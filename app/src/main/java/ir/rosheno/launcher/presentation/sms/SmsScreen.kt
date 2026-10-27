package ir.rosheno.launcher.presentation.sms

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.model.SmsSendReport
import ir.rosheno.launcher.domain.phone.comparableSmsAddress
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.phone.persianDigits
import ir.rosheno.launcher.domain.sms.messageThreads
import ir.rosheno.launcher.domain.sms.smsPartCount
import ir.rosheno.launcher.presentation.common.resolve
import ir.rosheno.launcher.presentation.components.LauncherIcon
import ir.rosheno.launcher.presentation.theme.Green
import ir.rosheno.launcher.presentation.theme.Hairline
import ir.rosheno.launcher.presentation.theme.Muted
import ir.rosheno.launcher.presentation.theme.Pale

@Composable
fun SmsScreen(contacts: List<FamilyContact>, size: Int, onHome: () -> Unit, openSystemMessages: () -> Unit,
    vm: SmsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val mode = state.mode
    val confirmationBody = state.confirmationBody
    val textSize = 22 + size * 3
    val waiting = state.submitting || state.report?.let { !it.complete && !state.reportExpired } == true
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) vm.start() else vm.error(context.getString(R.string.sms_read_permission_denied))
    }
    val sendPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) vm.confirmDraft() else vm.error(context.getString(R.string.sms_send_permission_denied))
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
    BackHandler(mode != "inbox", onBack = vm::back)
    val threads = remember(state.messages) { messageThreads(state.messages) }
    val thread = threads.find { it.id == state.threadId }
    fun sender(address: String): String = contacts.find {
        comparableSmsAddress(it.number) == comparableSmsAddress(address)
    }?.name ?: address.ifBlank { context.getString(R.string.sms_unknown_number) }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (mode == "compose") context.getString(R.string.sms_compose_title) else if (mode == "thread") thread?.let { sender(it.address) } ?: context.getString(R.string.sms_conversation_title) else context.getString(R.string.sms_inbox_title),
            fontSize = textSize.sp, fontWeight = FontWeight.Bold)
        state.error?.let { error -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(error.resolve(context), fontSize = 18.sp)
                TextButton(onClick = { vm.error(null) }) { Text(context.getString(R.string.action_understood)) }
            }
        } }
        state.report?.let { report -> Text(smsReportLabel(report, context), fontSize = 18.sp, color = if (report.failed) MaterialTheme.colorScheme.error else Green,
            modifier = Modifier.testTag("sms-send-status")) }
        if (mode == "compose") {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(state.recipient, { vm.draft(recipient = it) }, label = { Text(context.getString(R.string.sms_recipient_label)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !waiting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = LocalTextStyle.current.copy(fontSize = textSize.sp, textDirection = TextDirection.Ltr))
                if (contacts.isNotEmpty()) OutlinedButton(onClick = { vm.chooseContact(true) }, enabled = !waiting) { Text(context.getString(R.string.action_choose_family_contact), fontSize = 20.sp) }
                OutlinedTextField(state.draft, { vm.draft(text = it) }, label = { Text(context.getString(R.string.sms_body_label)) }, minLines = 4,
                    modifier = Modifier.fillMaxWidth(), enabled = !waiting,
                    textStyle = LocalTextStyle.current.copy(fontSize = textSize.sp))
                Text(context.getString(R.string.sms_part_count, persianDigits(smsPartCount(state.draft).toString())), fontSize = 16.sp, color = Muted)
                Button(enabled = !waiting && normalizePhone(state.recipient) != null && state.draft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), onClick = {
                        if (vm.hasPermission(Manifest.permission.SEND_SMS)) vm.confirmDraft()
                        else sendPermission.launch(Manifest.permission.SEND_SMS)
                    }) { Text(if (waiting) context.getString(R.string.status_sending_sms) else context.getString(R.string.action_review_send), fontSize = 22.sp) }
                OutlinedButton(onClick = { vm.inbox() }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(context.getString(R.string.action_all_conversations), fontSize = 20.sp) }
                if (state.draft.isNotBlank()) TextButton(onClick = { vm.clearDraft(true) }, enabled = !waiting) { Text(context.getString(R.string.action_clear_draft), fontSize = 18.sp) }
            }
        } else {
            Button(onClick = {
                vm.compose(if (mode == "thread") thread?.address else null)
            }, enabled = !waiting && (mode != "thread" || normalizePhone(thread?.address.orEmpty()) != null),
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text(if (mode == "thread") context.getString(R.string.action_reply) else if (state.draft.isNotBlank()) context.getString(R.string.action_continue_draft) else context.getString(R.string.action_new_message), fontSize = 22.sp)
            }
            if (!state.canRead) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LauncherIcon("message", Green)
                    Text(context.getString(R.string.sms_permission_description), fontSize = 22.sp)
                    Button(onClick = { readPermission.launch(Manifest.permission.READ_SMS) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                        Text(context.getString(R.string.action_sms_read_permission), fontSize = 20.sp)
                    }
                    OutlinedButton(onClick = openSystemMessages, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(context.getString(R.string.action_open_system_sms), fontSize = 20.sp) }
                    TextButton(onClick = {
                        runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null))) }
                            .onFailure { vm.error(context.getString(R.string.error_sms_permission_settings)) }
                    }) { Text(context.getString(R.string.action_sms_permission_settings), fontSize = 18.sp) }
                }
            } else {
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mode == "thread") {
                        item { OutlinedButton(onClick = { vm.inbox() }) { Text(context.getString(R.string.action_all_conversations), fontSize = 20.sp) } }
                        if (thread == null && !state.loading) item { Text(context.getString(R.string.sms_thread_empty), fontSize = 22.sp) }
                        items(thread?.messages?.sortedBy { it.date }.orEmpty(), key = { it.id }) { message ->
                            Surface(shape = RoundedCornerShape(20.dp), color = if (message.type == TelephonyType.INBOX) Pale else MaterialTheme.colorScheme.surface) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(smsTypeLabel(message.type, context), fontSize = 16.sp, color = Muted)
                                    SelectionContainer { Text(message.body.ifEmpty { context.getString(R.string.sms_empty_body) }, fontSize = textSize.sp) }
                                    Text(smsDate(message.date), fontSize = 16.sp, color = Muted)
                                }
                            }
                        }
                    } else {
                        if (threads.isEmpty() && !state.loading) item { Text(context.getString(R.string.sms_inbox_empty), fontSize = 22.sp) }
                        items(threads, key = { it.id }) { conversation ->
                            Surface(onClick = { vm.openThread(conversation.id) }, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Hairline)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(sender(conversation.address), fontSize = textSize.sp, fontWeight = FontWeight.Bold)
                                    Text(conversation.messages.first().body, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(smsDate(conversation.messages.first().date), fontSize = 16.sp, color = Muted)
                                }
                            }
                        }
                    }
                    if (state.hasMore) item { OutlinedButton(onClick = vm::more, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text(context.getString(R.string.action_load_older_sms), fontSize = 20.sp) } }
                    if (state.error != null) item { OutlinedButton(onClick = openSystemMessages, modifier = Modifier.fillMaxWidth()) { Text(context.getString(R.string.action_open_system_sms), fontSize = 20.sp) } }
                }
            }
        }
    }
    state.confirmationAddress?.let { address -> AlertDialog(onDismissRequest = { vm.dismissConfirmation() },
        title = { Text(context.getString(R.string.sms_send_confirmation_title), fontSize = 24.sp) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(address, style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr), fontSize = 22.sp)
            Text(confirmationBody, fontSize = 22.sp)
            Text(context.getString(R.string.sms_confirmation_part_count, persianDigits(smsPartCount(confirmationBody).toString())), fontSize = 18.sp)
        } },
        confirmButton = { TextButton(onClick = { vm.sendConfirmation() }) { Text(context.getString(R.string.action_send_sms), fontSize = 22.sp) } },
        dismissButton = { TextButton(onClick = { vm.dismissConfirmation() }) { Text(context.getString(R.string.action_cancel), fontSize = 22.sp) } }) }
    if (state.choosingContact) AlertDialog(onDismissRequest = { vm.chooseContact(false) }, title = { Text(context.getString(R.string.sms_choose_recipient_title)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            contacts.forEach { contact -> OutlinedButton(onClick = { vm.selectContact(contact.number) }, modifier = Modifier.fillMaxWidth()) { Text(contact.name, fontSize = 22.sp) } }
        } }, confirmButton = { TextButton(onClick = { vm.chooseContact(false) }) { Text(context.getString(R.string.action_back)) } })
    state.replaceDraftFor?.let { address -> AlertDialog(onDismissRequest = { vm.dismissReplaceDraft() }, title = { Text(context.getString(R.string.sms_existing_draft_title)) },
        text = { Text(context.getString(R.string.sms_existing_draft_description), fontSize = 20.sp) },
        confirmButton = { TextButton(onClick = { vm.replaceDraft(true) }) { Text(context.getString(R.string.action_replace_draft)) } },
        dismissButton = { TextButton(onClick = { vm.replaceDraft(false) }) { Text(context.getString(R.string.action_keep_draft)) } }) }
    if (state.clearingDraft) AlertDialog(onDismissRequest = { vm.clearDraft(false) }, title = { Text(context.getString(R.string.sms_clear_draft_title)) },
        confirmButton = { TextButton(onClick = { vm.confirmClearDraft() }) { Text(context.getString(R.string.action_clear)) } },
        dismissButton = { TextButton(onClick = { vm.clearDraft(false) }) { Text(context.getString(R.string.action_cancel)) } })
}

private object TelephonyType { const val INBOX = 1 }
private fun smsDate(date: Long): String = android.icu.text.DateFormat.getInstanceForSkeleton("MMMdHm",
    android.icu.util.ULocale("fa_IR@calendar=persian")).format(java.util.Date(date))
private fun smsTypeLabel(type: Int, context: android.content.Context): String = when (type) {
    1 -> context.getString(R.string.sms_type_inbox); 2 -> context.getString(R.string.sms_type_sent); 3 -> context.getString(R.string.sms_type_draft); 4, 6 -> context.getString(R.string.sms_type_pending); 5 -> context.getString(R.string.sms_type_failed); else -> context.getString(R.string.sms_type_other)
}
fun smsReportLabel(report: SmsSendReport, context: android.content.Context): String = when {
    report.failed && report.results.any { it == android.app.Activity.RESULT_OK } -> context.getString(R.string.sms_report_partial_failure)
    report.failed -> context.getString(R.string.sms_report_failure)
    report.sent -> context.getString(R.string.sms_report_sent)
    report.expired -> context.getString(R.string.sms_report_unknown)
    else -> context.getString(R.string.sms_report_sending)
}
