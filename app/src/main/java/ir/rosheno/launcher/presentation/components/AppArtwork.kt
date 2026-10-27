package ir.rosheno.launcher.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import ir.rosheno.launcher.domain.model.LaunchableApp
import ir.rosheno.launcher.presentation.theme.Pale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppArtwork(app: LaunchableApp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, app.key) {
        value = withContext(Dispatchers.IO) { runCatching {
            context.packageManager.getActivityIcon(android.content.ComponentName.unflattenFromString(app.key) ?: error("Invalid app key")).toBitmap(96, 96).asImageBitmap()
        }.getOrNull() }
    }
    Box(modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Pale)
        .testTag("app-icon-frame-${app.key}")) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp)
            .testTag("app-icon-${app.key}")) }
    }
}
