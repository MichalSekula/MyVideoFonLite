package pl.sekula.myvideofonlite.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta marki: granat + morski akcent + bursztyn dla akcji furtki.
val Navy900 = Color(0xFF0B1120)
val Navy800 = Color(0xFF111A2E)
val Navy700 = Color(0xFF1A2540)
val Navy600 = Color(0xFF24324F)
val Teal400 = Color(0xFF2DD4BF)
val Teal600 = Color(0xFF0D9488)
val Amber400 = Color(0xFFFBBF24)
val Amber600 = Color(0xFFD97706)
val Coral500 = Color(0xFFF87171)

val StatusOnline = Color(0xFF22C55E)
val StatusOffline = Color(0xFFEF4444)
val StatusUnknown = Color(0xFF94A3B8)

/** Gradient głównej karty urządzenia – taki sam w jasnym i ciemnym motywie. */
val HeroGradient = Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF134E4A), Color(0xFF0D9488)))

private val DarkColors = darkColorScheme(
    primary = Teal400,
    onPrimary = Navy900,
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Amber400,
    onSecondary = Navy900,
    secondaryContainer = Color(0xFF3F2E0B),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF93C5FD),
    background = Navy900,
    onBackground = Color(0xFFE2E8F0),
    surface = Navy900,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Navy700,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Navy800,
    surfaceContainerHigh = Navy700,
    surfaceContainerHighest = Navy600,
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = Coral500,
)

private val LightColors = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Amber600,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF2563EB),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
)

private val AppTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun MyVideoFonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
