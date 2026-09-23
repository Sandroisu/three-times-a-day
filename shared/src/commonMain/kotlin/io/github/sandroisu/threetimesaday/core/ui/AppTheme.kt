package io.github.sandroisu.threetimesaday.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object AppSpacing {
    val small = 4.dp
    val compact = 8.dp
    val medium = 12.dp
    val standard = 16.dp
    val section = 24.dp
    val large = 32.dp
}

@Immutable
internal data class AppSemanticColors(
    val success: Color,
    val successContainer: Color,
    val attention: Color,
)

private val lightAppSemanticColors = AppSemanticColors(
    success = Color(0xFF326453),
    successContainer = Color(0xFFEAF2ED),
    attention = Color(0xFFE9A23B),
)

private val LocalAppSemanticColors = staticCompositionLocalOf { lightAppSemanticColors }

internal val MaterialTheme.semanticColors: AppSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppSemanticColors.current

private val appColors = lightColorScheme(
    primary = Color(0xFF246B68),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F0ED),
    onPrimaryContainer = Color(0xFF194D4A),
    secondary = Color(0xFF566763),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF0EE),
    onSecondaryContainer = Color(0xFF354A45),
    background = Color(0xFFF5F6F5),
    onBackground = Color(0xFF202A29),
    surface = Color.White,
    onSurface = Color(0xFF202A29),
    surfaceVariant = Color(0xFFEAF0EE),
    onSurfaceVariant = Color(0xFF586562),
    surfaceContainer = Color(0xFFEDF0EE),
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFE7ECE9),
    surfaceContainerHighest = Color(0xFFE0E6E3),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF78847F),
    outlineVariant = Color(0xFFDFE5E1),
    error = Color(0xFF984B43),
    onError = Color.White,
    errorContainer = Color(0xFFF8EAE6),
    onErrorContainer = Color(0xFF753D36),
)

private fun appTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
): TextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private val appTypography = Typography(
    headlineLarge = appTextStyle(28, 36, FontWeight.SemiBold),
    headlineMedium = appTextStyle(26, 34, FontWeight.SemiBold),
    headlineSmall = appTextStyle(24, 32, FontWeight.SemiBold),
    titleLarge = appTextStyle(20, 28, FontWeight.SemiBold),
    titleMedium = appTextStyle(18, 26, FontWeight.SemiBold),
    titleSmall = appTextStyle(16, 24, FontWeight.Medium),
    bodyLarge = appTextStyle(16, 24, FontWeight.Normal),
    bodyMedium = appTextStyle(14, 22, FontWeight.Normal),
    bodySmall = appTextStyle(12, 18, FontWeight.Normal),
    labelLarge = appTextStyle(14, 20, FontWeight.SemiBold),
    labelMedium = appTextStyle(12, 18, FontWeight.Medium),
    labelSmall = appTextStyle(12, 18, FontWeight.Medium),
)

@Composable
internal fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppSemanticColors provides lightAppSemanticColors) {
        MaterialTheme(
            colorScheme = appColors,
            typography = appTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(24.dp),
            ),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                content()
            }
        }
    }
}
