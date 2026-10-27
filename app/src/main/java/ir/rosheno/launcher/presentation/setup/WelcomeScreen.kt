package ir.rosheno.launcher.presentation.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.phone.persianDigits
import ir.rosheno.launcher.platform.PlatformActions
import ir.rosheno.launcher.presentation.components.Action
import ir.rosheno.launcher.presentation.components.Body
import ir.rosheno.launcher.presentation.components.Title
import ir.rosheno.launcher.presentation.launcher.LauncherViewModel
import ir.rosheno.launcher.presentation.settings.SizePicker
import ir.rosheno.launcher.presentation.theme.Pale

@Composable internal fun Welcome(config: LauncherConfig, homeSelected: Boolean, platform: PlatformActions,
    model: LauncherViewModel, add: () -> Unit) {
    val context = LocalContext.current
    Title(context.getString(R.string.welcome_title))
    Body(context.getString(R.string.welcome_description))
    Surface(shape = RoundedCornerShape(24.dp), color = Pale) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Title(context.getString(R.string.welcome_home_step))
            Body(if (homeSelected) context.getString(R.string.welcome_home_selected) else context.getString(R.string.welcome_home_description))
            Action(if (homeSelected) context.getString(R.string.action_check_home) else context.getString(R.string.action_choose_rosheno), platform::chooseHome)
        }
    }
    Title(context.getString(R.string.welcome_family_step))
    Body(if (config.contacts.isEmpty()) context.getString(R.string.welcome_family_empty) else context.getString(R.string.welcome_family_count, persianDigits(config.contacts.size.toString())))
    if (config.contacts.size < 9) Action(context.getString(R.string.action_add_person), add, secondary = true)
    Title(context.getString(R.string.welcome_text_size_step))
    SizePicker(config.size, model::setSize)
    Action(context.getString(R.string.action_start_using), model::completeSetup)
    TextButton(onClick = platform::homeSettings) { Text(context.getString(R.string.action_choose_other_home), fontSize = 18.sp) }
}
