package ir.rosheno.launcher.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.rosheno.launcher.presentation.components.LauncherIcon
import ir.rosheno.launcher.presentation.theme.Green
import ir.rosheno.launcher.presentation.theme.Ink
import ir.rosheno.launcher.presentation.theme.Muted
import ir.rosheno.launcher.presentation.theme.Pale

internal val LocalActionTextSize = compositionLocalOf { 22 }

@Composable internal fun Title(text: String) = Text(text, fontSize = LocalActionTextSize.current.sp,
    fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.semantics { heading() })

@Composable internal fun Body(text: String) = Text(text, fontSize = (LocalActionTextSize.current - 2).sp,
    lineHeight = (LocalActionTextSize.current + 10).sp, color = Muted)

@Composable internal fun PhoneNumber(text: String) = Text(text, fontSize = LocalActionTextSize.current.sp,
    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))

@Composable internal fun Empty(text: String) = Surface(color = Pale, shape = RoundedCornerShape(20.dp)) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LauncherIcon("family", Green)
        Text(text, fontSize = LocalActionTextSize.current.sp, textAlign = TextAlign.Center)
    }
}

@Composable internal fun Action(text: String, click: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false) {
    val actionModifier = modifier.fillMaxWidth().heightIn(min = 72.dp)
    if (secondary) OutlinedButton(onClick = click, modifier = actionModifier, shape = RoundedCornerShape(18.dp)) {
        Text(text, fontSize = LocalActionTextSize.current.sp, textAlign = TextAlign.Center)
    } else Button(onClick = click, modifier = actionModifier, shape = RoundedCornerShape(18.dp)) {
        Text(text, fontSize = LocalActionTextSize.current.sp, textAlign = TextAlign.Center)
    }
}
