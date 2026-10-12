package name.lechners.sudomnia.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import name.lechners.sudomnia.data.ThemeMode
import androidx.compose.ui.graphics.Color

private fun scheme(p: Palette, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = LogoBlue,
        onPrimary = Color.White,
        secondary = LogoBlue,
        background = p.appBackground,
        onBackground = p.textPrimary,
        surface = p.appSurface,
        onSurface = p.textPrimary,
        surfaceVariant = p.appSurfaceHigh,
        onSurfaceVariant = p.textSecondary,
        outline = p.appOutline,
    )
} else {
    lightColorScheme(
        primary = LogoBlue,
        onPrimary = Color.White,
        secondary = LogoBlue,
        background = p.appBackground,
        onBackground = p.textPrimary,
        surface = p.appSurface,
        onSurface = p.textPrimary,
        surfaceVariant = p.appSurfaceHigh,
        onSurfaceVariant = p.textSecondary,
        outline = p.appOutline,
    )
}

/** The grid takes the same side as the chrome; [mode] decides which, SYSTEM follows the device. */
@Composable
fun SudomniaTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    // Edge-to-edge picks the status and navigation bar icon colour from the system
    // setting; a manual choice that disagrees with it would leave unreadable icons.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // The bars are transparent and show the window background, which
            // themes.xml ties to the system mode, not to this choice.
            window.setBackgroundDrawable(ColorDrawable(palette.appBackground.toArgb()))
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme(palette, dark), content = content)
    }
}
