package com.status.simplecompass.ui.theme

import android.os.Build
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.status.simplecompass.data.ThemeMode
import com.status.simplecompass.data.ThemePalette

private val ClassicLight = lightColorScheme(
    primary = Color(0xFFE45745),
    onPrimary = Color.White,
    secondary = Color(0xFF1F6F78),
    background = Color(0xFFF7F3E8),
    onBackground = Color(0xFF102A43),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF102A43),
    surfaceVariant = Color(0xFFE8E1D2),
    onSurfaceVariant = Color(0xFF52616B),
    error = Color(0xFFB3261E),
)

private val ClassicDark = darkColorScheme(
    primary = Color(0xFFFF7967),
    onPrimary = Color(0xFF4A0B03),
    secondary = Color(0xFF74D1D9),
    background = Color(0xFF091A2A),
    onBackground = Color(0xFFF2ECDD),
    surface = Color(0xFF102A43),
    onSurface = Color(0xFFF2ECDD),
    surfaceVariant = Color(0xFF263F54),
    onSurfaceVariant = Color(0xFFC3CCD3),
    error = Color(0xFFFFB4AB),
)

private val OceanLight = lightColorScheme(
    primary = Color(0xFF006D8F),
    secondary = Color(0xFF32748A),
    background = Color(0xFFF2FAFD),
    surface = Color(0xFFF8FCFF),
    surfaceVariant = Color(0xFFD7E8EF),
    onBackground = Color(0xFF0B2733),
    onSurface = Color(0xFF0B2733),
    onSurfaceVariant = Color(0xFF405F6B),
)

private val OceanDark = darkColorScheme(
    primary = Color(0xFF5BD5FC),
    secondary = Color(0xFF82CDDF),
    background = Color(0xFF051D27),
    surface = Color(0xFF0B2A37),
    surfaceVariant = Color(0xFF244654),
    onBackground = Color(0xFFD9F4FF),
    onSurface = Color(0xFFD9F4FF),
    onSurfaceVariant = Color(0xFFB6CBD3),
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF39744A),
    secondary = Color(0xFF687B43),
    background = Color(0xFFF4F7EC),
    surface = Color(0xFFFBFCF5),
    surfaceVariant = Color(0xFFDFE7D5),
    onBackground = Color(0xFF1A2C20),
    onSurface = Color(0xFF1A2C20),
    onSurfaceVariant = Color(0xFF536052),
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFF91D6A1),
    secondary = Color(0xFFBECE8C),
    background = Color(0xFF101F15),
    surface = Color(0xFF192C20),
    surfaceVariant = Color(0xFF344B38),
    onBackground = Color(0xFFDCEBDD),
    onSurface = Color(0xFFDCEBDD),
    onSurfaceVariant = Color(0xFFBDCABE),
)

private val SunsetLight = lightColorScheme(
    primary = Color(0xFFB6466A),
    secondary = Color(0xFF9A5C13),
    background = Color(0xFFFFF6F0),
    surface = Color(0xFFFFFBF8),
    surfaceVariant = Color(0xFFF5DFD5),
    onBackground = Color(0xFF3A2027),
    onSurface = Color(0xFF3A2027),
    onSurfaceVariant = Color(0xFF70565B),
)

private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFA8C2),
    secondary = Color(0xFFFFB95C),
    background = Color(0xFF28131A),
    surface = Color(0xFF3A2027),
    surfaceVariant = Color(0xFF60404A),
    onBackground = Color(0xFFFFE1E8),
    onSurface = Color(0xFFFFE1E8),
    onSurfaceVariant = Color(0xFFE1BEC7),
)

private val HighContrastLight = lightColorScheme(
    primary = Color(0xFF0033CC),
    onPrimary = Color.White,
    secondary = Color(0xFF750000),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
)

private val HighContrastDark = darkColorScheme(
    primary = Color(0xFFFFD600),
    onPrimary = Color.Black,
    secondary = Color(0xFF7FDBFF),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF101010),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF292929),
    onSurfaceVariant = Color.White,
    outline = Color.White,
)

private val CompassTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        letterSpacing = (-2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
)

@Composable
fun SimpleCompassTheme(
    themeMode: ThemeMode,
    palette: ThemePalette,
    window: Window? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> paletteColors(palette, darkTheme)
    }

    MaterialTheme(
        colorScheme = colors,
        typography = CompassTypography,
    ) {
        ApplySystemBars(window, darkTheme, colors)
        content()
    }
}

@Composable
fun palettePreviewColor(palette: ThemePalette, darkTheme: Boolean): Color {
    val context = LocalContext.current
    return if (palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context).primary
        else dynamicLightColorScheme(context).primary
    } else {
        paletteColors(palette, darkTheme).primary
    }
}

private fun paletteColors(palette: ThemePalette, darkTheme: Boolean): ColorScheme = when (palette) {
    ThemePalette.CLASSIC, ThemePalette.DYNAMIC -> if (darkTheme) ClassicDark else ClassicLight
    ThemePalette.OCEAN -> if (darkTheme) OceanDark else OceanLight
    ThemePalette.FOREST -> if (darkTheme) ForestDark else ForestLight
    ThemePalette.SUNSET -> if (darkTheme) SunsetDark else SunsetLight
    ThemePalette.HIGH_CONTRAST -> if (darkTheme) HighContrastDark else HighContrastLight
}

@Composable
private fun ApplySystemBars(window: Window?, darkTheme: Boolean, colors: ColorScheme) {
    val view = LocalView.current
    SideEffect {
        window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.Transparent.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = colors.background.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}
