package ir.rosheno.launcher.presentation.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.platform.PlatformActions
import ir.rosheno.launcher.presentation.components.AppArtwork
import ir.rosheno.launcher.presentation.components.Body
import ir.rosheno.launcher.presentation.components.Empty
import ir.rosheno.launcher.presentation.components.LocalActionTextSize
import ir.rosheno.launcher.presentation.components.Title
import ir.rosheno.launcher.presentation.launcher.LauncherState
import ir.rosheno.launcher.presentation.launcher.LauncherViewModel

@Composable internal fun AppDirectory(state: LauncherState, model: LauncherViewModel, platform: PlatformActions,
    directory: AppDirectoryViewModel = viewModel()) {
    val context = LocalContext.current
    val query by directory.query.collectAsStateWithLifecycle()
    Title(context.getString(R.string.apps_title))
    Body(context.getString(R.string.apps_description))
    OutlinedTextField(value = query, onValueChange = directory::search, label = { Text(context.getString(R.string.apps_search)) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = LocalActionTextSize.current.sp))
    val apps = directory.filter(state.apps)
    if (apps.isEmpty()) Empty(context.getString(R.string.apps_empty))
    apps.forEach { app -> Surface(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { platform.launch(app.key) }, modifier = Modifier.fillMaxWidth().testTag("app-${app.key}")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppArtwork(app)
                    Text(app.label, fontSize = LocalActionTextSize.current.sp, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
                }
            }
            val selected = app.key in state.config.favorites
            OutlinedButton(onClick = { model.toggleFavorite(app.key) },
                enabled = selected || state.config.favorites.size < 8) {
                Text(if (selected) context.getString(R.string.action_remove_favorite) else context.getString(R.string.action_add_favorite), fontSize = 18.sp)
            }
        }
    } }
}
