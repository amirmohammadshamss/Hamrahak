package ir.rosheno.launcher.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ir.rosheno.launcher.R

internal val Ink: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.ink)
internal val Green: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.brand_green)
internal val Paper: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.warm_paper)
internal val Muted: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.muted)
internal val Pale: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.pale)
internal val Orange: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.orange)
internal val Hairline: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.hairline)

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
            primary = Green, onPrimary = colorResource(R.color.white), background = Paper, onBackground = Ink,
            surface = colorResource(R.color.white), onSurface = Ink, secondaryContainer = Pale,
            onSecondaryContainer = Ink, error = colorResource(R.color.error_red), outline = Muted,
        ),
        typography = PersianTypography,
        shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(28.dp)),
        content = content,
    )
}
