package ir.rosheno.launcher.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.platform.PlatformActions
import ir.rosheno.launcher.presentation.components.Action
import ir.rosheno.launcher.presentation.components.Body
import ir.rosheno.launcher.presentation.components.Empty
import ir.rosheno.launcher.presentation.components.LocalActionTextSize
import ir.rosheno.launcher.presentation.components.PhoneNumber
import ir.rosheno.launcher.presentation.components.Title
import ir.rosheno.launcher.presentation.launcher.LauncherState
import ir.rosheno.launcher.presentation.launcher.LauncherViewModel
import ir.rosheno.launcher.presentation.theme.Pale

@Composable internal fun Settings(state: LauncherState, model: LauncherViewModel, platform: PlatformActions,
    homeSelected: Boolean, add: () -> Unit, edit: (FamilyContact) -> Unit, reset: () -> Unit, settings: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val deletingId by settings.deletingId.collectAsStateWithLifecycle()
    Title(context.getString(R.string.settings_family_title))
    Body(context.getString(R.string.settings_consent_description))
    Title(context.getString(R.string.text_size_title))
    SizePicker(state.config.size, model::setSize)
    Title(context.getString(R.string.settings_family_help_title))
    Body(context.getString(R.string.settings_help_description))
    if (state.config.contacts.isEmpty()) Empty(context.getString(R.string.settings_add_contact_hint))
    state.config.contacts.forEachIndexed { index, c -> Surface(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(c.name, fontSize = LocalActionTextSize.current.sp, fontWeight = FontWeight.Bold)
            PhoneNumber(c.number)
            Action(if (c.id == state.config.helpContactId) context.getString(R.string.help_contact_selected) else context.getString(R.string.action_select_help_contact),
                { model.setHelp(c.id) }, secondary = true)
            Action(context.getString(R.string.family_edit_action), { edit(c) }, modifier = Modifier.testTag("edit-contact-${c.id}"), secondary = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { model.moveContact(c.id, -1) }, enabled = index > 0,
                    modifier = Modifier.weight(1f).testTag("contact-up-${c.id}")) { Text(context.getString(R.string.action_move_up), fontSize = 18.sp) }
                OutlinedButton(onClick = { model.moveContact(c.id, 1) }, enabled = index < state.config.contacts.lastIndex,
                    modifier = Modifier.weight(1f).testTag("contact-down-${c.id}")) { Text(context.getString(R.string.action_move_down), fontSize = 18.sp) }
            }
            TextButton(onClick = { settings.requestDelete(c.id) }) { Text(context.getString(R.string.action_remove_from_rosheno), fontSize = 18.sp) }
            if (deletingId == c.id) AlertDialog(onDismissRequest = { settings.dismissDelete() },
                title = { Title(context.getString(R.string.contact_delete_confirmation_title, c.name)) }, text = { Body(context.getString(R.string.contact_delete_confirmation_description)) },
                confirmButton = { TextButton(onClick = { settings.consumeDelete()?.let(model::removeContact) }) { Text(context.getString(R.string.action_delete)) } },
                dismissButton = { TextButton(onClick = { settings.dismissDelete() }) { Text(context.getString(R.string.action_cancel)) } })
        }
    } }
    if (state.config.contacts.size < 9) Action(context.getString(R.string.action_add_person), add, secondary = true)
    if (state.config.favorites.isNotEmpty()) {
        Title(context.getString(R.string.settings_favorites_order))
        state.config.favorites.forEachIndexed { index, key ->
            Text(state.apps.find { it.key == key }?.label ?: context.getString(R.string.app_unavailable), fontSize = LocalActionTextSize.current.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { model.moveFavorite(key, -1) }, enabled = index > 0) { Text(context.getString(R.string.action_move_up)) }
                OutlinedButton(onClick = { model.moveFavorite(key, 1) }, enabled = index < state.config.favorites.lastIndex) { Text(context.getString(R.string.action_move_down)) }
                TextButton(onClick = { model.toggleFavorite(key) }) { Text(context.getString(R.string.action_delete)) }
            }
        }
    }
    Title(context.getString(R.string.settings_home_title))
    Body(if (homeSelected) context.getString(R.string.home_is_selected) else context.getString(R.string.home_not_selected))
    Action(context.getString(R.string.action_choose_rosheno), platform::chooseHome, secondary = true)
    Body(context.getString(R.string.settings_exit_description))
    Action(context.getString(R.string.action_choose_other_home), platform::homeSettings, secondary = true)
    HorizontalDivider()
    Body(context.getString(R.string.settings_prototype_description))
    TextButton(onClick = reset) { Text(context.getString(R.string.action_reset_settings), fontSize = 20.sp, color = MaterialTheme.colorScheme.error) }
}

@Composable internal fun SizePicker(selected: Int, change: (Int) -> Unit) {
    val context = LocalContext.current
    listOf(context.getString(R.string.text_size_readable), context.getString(R.string.text_size_large), context.getString(R.string.text_size_extra_large)).forEachIndexed { index, label ->
        Surface(color = if (selected == index) Pale else colorResource(R.color.white),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .selectable(selected == index, role = Role.RadioButton, onClick = { change(index) })) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == index, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(label, fontSize = (22 + index * 3).sp, modifier = Modifier.weight(1f))
            }
        }
    }
}
