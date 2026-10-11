package name.lechners.sudomnia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

/** Follows the system setting; the grid takes the same side as the chrome. */
@Composable
fun SudomniaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme(palette, dark), content = content)
    }
}
