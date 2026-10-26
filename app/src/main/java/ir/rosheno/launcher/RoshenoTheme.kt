package ir.rosheno.launcher

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

internal val Ink = Color(0xFF193F38)
internal val Green = Color(0xFF19594E)
internal val Paper = Color(0xFFF7F6F0)
internal val Muted = Color(0xFF51675F)
internal val Pale = Color(0xFFE7EFE8)
internal val Orange = Color(0xFF854521)
internal val Hairline = Color(0xFFE2E7DF)

// One bundled variable font; platform font synthesis supplies heavier weights.
private val PersianFont = FontFamily(Font(R.font.vazirmatn))
private val base = Typography()
private val PersianTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = PersianFont),
    displayMedium = base.displayMedium.copy(fontFamily = PersianFont),
    displaySmall = base.displaySmall.copy(fontFamily = PersianFont),
    headlineLarge = base.headlineLarge.copy(fontFamily = PersianFont),
    headlineMedium = base.headlineMedium.copy(fontFamily = PersianFont),
    headlineSmall = base.headlineSmall.copy(fontFamily = PersianFont),
    titleLarge = base.titleLarge.copy(fontFamily = PersianFont),
    titleMedium = base.titleMedium.copy(fontFamily = PersianFont),
    titleSmall = base.titleSmall.copy(fontFamily = PersianFont),
    bodyLarge = base.bodyLarge.copy(fontFamily = PersianFont),
    bodyMedium = base.bodyMedium.copy(fontFamily = PersianFont),
    bodySmall = base.bodySmall.copy(fontFamily = PersianFont),
    labelLarge = base.labelLarge.copy(fontFamily = PersianFont),
    labelMedium = base.labelMedium.copy(fontFamily = PersianFont),
    labelSmall = base.labelSmall.copy(fontFamily = PersianFont),
)

@Composable
fun RoshenoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green, onPrimary = Color.White, background = Paper, onBackground = Ink,
            surface = Color.White, onSurface = Ink, secondaryContainer = Pale,
            onSecondaryContainer = Ink, error = Color(0xFF9D3026), outline = Muted,
        ),
        typography = PersianTypography,
        shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(28.dp)),
        content = content,
    )
}
