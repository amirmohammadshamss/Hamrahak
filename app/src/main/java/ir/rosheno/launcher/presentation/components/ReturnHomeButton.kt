package ir.rosheno.launcher.presentation.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.rosheno.launcher.R

@Composable
internal fun ReturnHomeButton(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    minHeight: Dp = 64.dp,
) {
    val label = stringResource(R.string.action_return_to_rosheno)
    val buttonModifier = modifier.fillMaxWidth().heightIn(min = minHeight)
    val content: @Composable RowScope.() -> Unit = { Text(label, fontSize = 22.sp) }
    if (outlined) OutlinedButton(onClick = onHome, modifier = buttonModifier, content = content)
    else Button(onClick = onHome, modifier = buttonModifier, content = content)
}
