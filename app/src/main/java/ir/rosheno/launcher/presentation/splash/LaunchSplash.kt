package ir.rosheno.launcher.presentation.splash

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.rosheno.launcher.R
import ir.rosheno.launcher.presentation.theme.Green
import ir.rosheno.launcher.presentation.theme.Muted
import ir.rosheno.launcher.presentation.theme.Paper
import ir.rosheno.launcher.presentation.theme.RoshenoTheme

/** A brief, skippable brand entrance for manual cold launches, never the Home-key path. */
@Composable
fun LaunchSplash(onFinished: () -> Unit) {
    val context = LocalContext.current
    val reveal = remember { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        if (ValueAnimator.areAnimatorsEnabled()) reveal.animateTo(1f, tween(550))
        finish()
    }
    RoshenoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp)) {
                    Column(Modifier.align(Alignment.Center).alpha(reveal.value).scale(0.9f + reveal.value * 0.1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Image(painterResource(R.drawable.ic_launcher), contentDescription = null, Modifier.size(112.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.app_name), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Green)
                        Text(context.getString(R.string.brand_tagline), fontSize = 20.sp, color = Muted)
                    }
                    TextButton(onClick = { finish() }, modifier = Modifier.align(Alignment.BottomCenter)) {
                        Text(context.getString(R.string.action_start), fontSize = 20.sp)
                    }
                }
            }
        }
    }
}
