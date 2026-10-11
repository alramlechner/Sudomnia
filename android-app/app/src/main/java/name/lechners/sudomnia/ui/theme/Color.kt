package name.lechners.sudomnia.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App chrome: the Chessomnia palette, navy #2B303E to blue #3193C6. */
val LogoNavy = Color(0xFF2B303E)
val LogoBlue = Color(0xFF3193C6)

/**
 * Everything that has to change between the dark and the light scheme. The grid shares
 * the chrome's family in both: dark cells with light ink, or light cells with dark ink,
 * never a light sheet on a dark app. Every ink was picked for roughly 4:1 or better on
 * the backgrounds it can land on.
 */
class Palette(
    val appBackground: Color,
    val appSurface: Color,
    val appSurfaceHigh: Color,
    val appOutline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val good: Color,
    val bad: Color,
    val paperBase: Color,
    val paperGivenCell: Color,
    val paperSelected: Color,
    /**
     * Tint of the selected cell's row, column and box. Deliberately very weak: it covers
     * 21 cells and must not out-shout [paperSameDigit], which covers at most nine and is
     * what players actually look for.
     */
    val paperPeer: Color,
    /** Cells holding the same digit as the selected one. Has to win against [paperPeer]. */
    val paperSameDigit: Color,
    val paperConflict: Color,
    /** The cell a hint points at. Amber, because blue, green and red are taken. */
    val paperHint: Color,
    /** The unit that justifies a hint. Nine cells, so as weak as [paperPeer]. */
    val paperHintUnit: Color,
    /**
     * Cells written on trial, in an open branch. Yellow, the one hue still free. The
     * digits get [inkTrial] on top, so "provisional" survives selection and highlight.
     */
    val paperTrial: Color,
    val inkGiven: Color,
    val inkEntry: Color,
    val inkConflict: Color,
    val inkNote: Color,
    val inkTrial: Color,
    /** A pencil mark for the highlighted digit, same green family as [paperSameDigit]. */
    val inkNoteHighlight: Color,
    val gridLine: Color,
    val gridLineStrong: Color,
)

val DarkPalette = Palette(
    appBackground = Color(0xFF161A22),
    appSurface = Color(0xFF212734),
    appSurfaceHigh = Color(0xFF2B303E),
    appOutline = Color(0xFF3C4456),
    textPrimary = Color(0xFFE8ECF2),
    textSecondary = Color(0xFF9AA5B8),
    good = Color(0xFF6BC46D),
    bad = Color(0xFFE07A70),
    paperBase = Color(0xFF1E2431),
    paperGivenCell = Color(0xFF2A3142),
    paperSelected = Color(0xFF28506E),
    paperPeer = Color(0xFF262E3F),
    paperSameDigit = Color(0xFF355F2E),
    paperConflict = Color(0xFF6B2A2A),
    paperHint = Color(0xFF7A5C1A),
    paperHintUnit = Color(0xFF332F26),
    paperTrial = Color(0xFF5E5619),
    inkGiven = Color(0xFFE8ECF2),
    inkEntry = Color(0xFF7CC4F2),
    inkConflict = Color(0xFFFF9C94),
    inkNote = Color(0xFF8E99AB),
    inkTrial = Color(0xFFF2C45A),
    inkNoteHighlight = Color(0xFF9EDB7E),
    gridLine = Color(0xFF3C4456),
    gridLineStrong = Color(0xFF8A96AB),
)

val LightPalette = Palette(
    appBackground = Color(0xFFF3F5F9),
    appSurface = Color(0xFFFFFFFF),
    appSurfaceHigh = Color(0xFFE6EAF1),
    appOutline = Color(0xFFC5CCD8),
    textPrimary = Color(0xFF1C222E),
    textSecondary = Color(0xFF566176),
    good = Color(0xFF2E7D32),
    bad = Color(0xFFB3261E),
    paperBase = Color(0xFFFFFFFF),
    paperGivenCell = Color(0xFFE9ECF2),
    paperSelected = Color(0xFFBFDCEF),
    paperPeer = Color(0xFFEDF3F8),
    paperSameDigit = Color(0xFFA9D98A),
    paperConflict = Color(0xFFF2C0BC),
    paperHint = Color(0xFFF2C97A),
    paperHintUnit = Color(0xFFFBF0D8),
    paperTrial = Color(0xFFF2E27A),
    inkGiven = Color(0xFF1C222E),
    inkEntry = Color(0xFF1D6FA5),
    inkConflict = Color(0xFFA32B22),
    inkNote = Color(0xFF6B7480),
    inkTrial = Color(0xFF8A5A00),
    inkNoteHighlight = Color(0xFF2E6B12),
    gridLine = Color(0xFF9FA6B0),
    gridLineStrong = Color(0xFF2B303E),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

val AppBackground: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.appBackground
val AppSurface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.appSurface
val AppSurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.appSurfaceHigh
val AppOutline: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.appOutline
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textSecondary
val InkTrial: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.inkTrial
val InkConflict: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.inkConflict
