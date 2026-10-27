package ir.rosheno.launcher.presentation.help

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.presentation.components.Action
import ir.rosheno.launcher.presentation.components.Body
import ir.rosheno.launcher.presentation.components.Empty
import ir.rosheno.launcher.presentation.components.LauncherIcon
import ir.rosheno.launcher.presentation.components.LocalActionTextSize
import ir.rosheno.launcher.presentation.components.PhoneNumber
import ir.rosheno.launcher.presentation.components.Title
import ir.rosheno.launcher.presentation.family.ContactPhoto
import ir.rosheno.launcher.presentation.theme.Orange

@Composable internal fun HelpAction(trusted: FamilyContact?, click: () -> Unit) {
    val context = LocalContext.current
    Surface(onClick = click,
        color = colorResource(R.color.help_background), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, colorResource(R.color.help_outline)),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).background(colorResource(R.color.help_icon_background), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) { LauncherIcon("phone", Orange) }
            Column(Modifier.weight(1f)) {
                Text(context.getString(R.string.help_action_label), fontSize = LocalActionTextSize.current.sp, color = Orange, fontWeight = FontWeight.Bold)
                Text(trusted?.let { context.getString(R.string.call_contact_title, it.name) } ?: context.getString(R.string.help_contact_required), fontSize = 18.sp, color = Orange,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable internal fun HelpContactSetup(config: LauncherConfig, choose: (String) -> Unit, add: () -> Unit) {
    val context = LocalContext.current
    Title(context.getString(R.string.help_contact_title))
    Body(context.getString(R.string.help_contact_description))
    if (config.contacts.isEmpty()) {
        Empty(context.getString(R.string.help_contacts_empty))
        Action(context.getString(R.string.action_add_help_contact), add)
    } else {
        config.contacts.forEach { contact -> Surface(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ContactPhoto(contact.photo, contact.name)
                    Text(contact.name, fontSize = LocalActionTextSize.current.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                }
                PhoneNumber(contact.number)
                Action(context.getString(R.string.action_choose_help_contact), { choose(contact.id) }, modifier = Modifier.testTag("choose-help-${contact.id}"), secondary = true)
            }
        } }
        if (config.contacts.size < 9) Action(context.getString(R.string.action_add_another_contact), add, secondary = true)
    }
    Body(context.getString(R.string.help_contact_next_steps))
}
