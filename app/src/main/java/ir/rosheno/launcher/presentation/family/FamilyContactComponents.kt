package ir.rosheno.launcher.presentation.family

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.di.FeatureViewModelFactory
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.presentation.common.resolve
import ir.rosheno.launcher.presentation.components.Body
import ir.rosheno.launcher.presentation.components.LauncherIcon
import ir.rosheno.launcher.presentation.components.LocalActionTextSize
import ir.rosheno.launcher.presentation.components.Title
import ir.rosheno.launcher.presentation.theme.Green
import ir.rosheno.launcher.presentation.theme.Hairline
import ir.rosheno.launcher.presentation.theme.Pale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable internal fun ContactEditorDialog(initial: FamilyContact?, saving: Boolean, session: String,
    onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit, onError: (String) -> Unit,
    model: ContactEditorViewModel = viewModel(factory = FeatureViewModelFactory)) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(session) { model.begin(session, initial) }
    LaunchedEffect(state.error) { state.error?.let { onError(it.resolve(context)) } }
    val locked = state.busy || saving
    fun cancel() { if (!locked && model.cancel()) onDismiss() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !saving) model.stagePhoto(uri.toString())
    }
    AlertDialog(onDismissRequest = ::cancel,
        title = { Title(if (initial == null) context.getString(R.string.family_add_title) else context.getString(R.string.family_edit_title, initial.name)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Body(context.getString(R.string.family_edit_description))
            OutlinedTextField(state.name, model::name, label = { Text(context.getString(R.string.name_label)) }, singleLine = true,
                enabled = !locked, modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = LocalActionTextSize.current.sp))
            OutlinedTextField(state.number, model::number, label = { Text(context.getString(R.string.family_number_label)) }, singleLine = true,
                enabled = !locked, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = LocalTextStyle.current.copy(fontSize = LocalActionTextSize.current.sp, textDirection = TextDirection.Ltr),
                isError = state.number.isNotEmpty() && !state.validNumber,
                supportingText = { if (state.number.isNotEmpty() && !state.validNumber) Text(context.getString(R.string.phone_number_example)) })
            if (state.photo != null) ContactPhoto(state.photo, state.name)
            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !locked) {
                Text(if (state.busy) context.getString(R.string.status_preparing_photo) else if (state.photo == null) context.getString(R.string.action_choose_optional_photo) else context.getString(R.string.action_change_photo), fontSize = 18.sp)
            }
            if (state.photo != null) TextButton(onClick = model::removePhoto, enabled = !locked) {
                Text(context.getString(R.string.action_remove_photo), fontSize = 18.sp)
            }
        } },
        confirmButton = { TextButton(onClick = { onSave(state.name, state.number, state.photo) },
            enabled = state.canSave && !saving) {
            Text(if (saving) context.getString(R.string.status_saving) else context.getString(R.string.action_save), fontSize = 22.sp)
        } },
        dismissButton = { TextButton(onClick = ::cancel, enabled = !locked) { Text(context.getString(R.string.action_cancel), fontSize = 22.sp) } })
}

@Composable internal fun ContactRow(contact: FamilyContact, click: () -> Unit) {
    val context = LocalContext.current
    Surface(onClick = click, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Hairline),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ContactPhoto(contact.photo, contact.name)
            Column(Modifier.weight(1f)) {
                Text(contact.name, fontSize = LocalActionTextSize.current.sp, fontWeight = FontWeight.Bold)
                Text(context.getString(R.string.call_label), fontSize = 18.sp, color = Green)
            }
            LauncherIcon("phone", Green)
        }
    }
}

@Composable internal fun ContactPhoto(path: String?, name: String) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { path?.let { BitmapFactory.decodeFile(it) } }
    }
    Box(Modifier.size(72.dp).clip(CircleShape).background(Pale), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name.take(1), fontSize = 30.sp, color = Green)
    }
}
