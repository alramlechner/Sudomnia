package name.lechners.sudomnia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = LogoBlue,
    onPrimary = Color.White,
    secondary = LogoBlue,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = AppOutline,
)

/** One scheme only: the paper grid supplies the contrast, the chrome stays dark. */
@Composable
fun SudomniaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
