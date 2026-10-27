package ir.rosheno.launcher.presentation.home

import android.icu.text.DateFormat
import android.icu.util.ULocale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.platform.PlatformActions
import ir.rosheno.launcher.presentation.components.Action
import ir.rosheno.launcher.presentation.components.AppArtwork
import ir.rosheno.launcher.presentation.components.LauncherIcon
import ir.rosheno.launcher.presentation.components.LocalActionTextSize
import ir.rosheno.launcher.presentation.components.Title
import ir.rosheno.launcher.presentation.family.ContactPhoto
import ir.rosheno.launcher.presentation.launcher.LauncherState
import ir.rosheno.launcher.presentation.theme.Green
import ir.rosheno.launcher.presentation.theme.Hairline
import ir.rosheno.launcher.presentation.theme.Pale
import java.util.Date

@Composable internal fun Home(state: LauncherState, homeSelected: Boolean, navigate: (String) -> Unit,
    call: (String) -> Unit, platform: PlatformActions, clock: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val timestamp by clock.time.collectAsStateWithLifecycle()
    val now = remember(timestamp) { Date(timestamp) }
    val time = remember(now) { DateFormat.getInstanceForSkeleton("Hm", ULocale("fa_IR")).format(now) }
    val date = remember(now) { DateFormat.getInstanceForSkeleton("yMMMMEEEEd", ULocale("fa_IR@calendar=persian")).format(now) }
    Surface(color = Green, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(context.getString(R.string.home_today), fontSize = 16.sp, color = colorResource(R.color.on_brand_subtle))
            Text(time, fontSize = 52.sp, fontWeight = FontWeight.Medium, color = colorResource(R.color.white),
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))
            Text(date, fontSize = 18.sp, color = colorResource(R.color.white), textAlign = TextAlign.Center)
        }
    }
    if (!homeSelected) Surface(color = Pale, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = platform::chooseHome)) {
        Text(context.getString(R.string.home_choose_launcher_hint), Modifier.padding(16.dp), fontSize = 18.sp)
    }
    if (state.config.contacts.isNotEmpty()) {
        Title(context.getString(R.string.home_quick_call))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            state.config.contacts.take(2).forEach { contact ->
                Surface(onClick = { call(contact.id) }, shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 112.dp), color = Pale, border = BorderStroke(1.dp, Hairline)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContactPhoto(contact.photo, contact.name)
                        Text(contact.name, fontSize = LocalActionTextSize.current.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
    val tiles = listOf(
        Tile(context.getString(R.string.call_label), "phone", { navigate("dialer") }),
        Tile(context.getString(R.string.contacts_title), "family", { navigate("contacts") }),
        Tile(context.getString(R.string.family_title), "family", { navigate("family") }),
        Tile(context.getString(R.string.messages_label), "message", { navigate("messages") }),
        Tile(context.getString(R.string.photos_label), "photo", { navigate("photos") }),
        Tile(context.getString(R.string.apps_title), "apps", { navigate("apps") }),
        Tile(context.getString(R.string.camera_title), "camera", { navigate("camera") }),
    )
    tiles.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.forEach { tile -> HomeTile(tile, Modifier.weight(1f)) }
    } }
    if (state.config.favorites.isNotEmpty()) {
        Title(context.getString(R.string.home_favorite_apps))
        state.config.favorites.forEach { key ->
            val app = state.apps.find { it.key == key }
            if (app != null) Surface(onClick = { platform.launch(key) }, shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Hairline), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppArtwork(app)
                    Text(app.label, fontSize = LocalActionTextSize.current.sp, modifier = Modifier.weight(1f))
                }
            } else Action(context.getString(R.string.app_unavailable), { platform.launch(key) }, secondary = true)
        }
    }
    TextButton(onClick = { navigate("help") }, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.guide_usage_title), fontSize = 20.sp)
    }
}

private data class Tile(val label: String, val icon: String, val click: () -> Unit)

@Composable private fun HomeTile(tile: Tile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(100), label = "tile-press")
    val primary = tile.icon == "phone"
    val fill = if (primary) Green else colorResource(R.color.white)
    val ink = if (primary) colorResource(R.color.white) else Green
    Surface(onClick = tile.click, interactionSource = interactions, shape = RoundedCornerShape(24.dp),
        border = if (primary) null else BorderStroke(1.dp, Hairline), color = fill,
        modifier = modifier.scale(scale).heightIn(min = 136.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
            Box(Modifier.size(48.dp).background(if (primary) colorResource(R.color.white).copy(alpha = 0.12f) else Pale,
                RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { LauncherIcon(tile.icon, ink) }
            Text(tile.label, fontSize = LocalActionTextSize.current.sp, fontWeight = FontWeight.SemiBold,
                color = ink, textAlign = TextAlign.Center)
        }
    }
}
