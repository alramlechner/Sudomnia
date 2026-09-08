package name.lechners.sudomnia.ui.theme

import androidx.compose.ui.graphics.Color

/** App chrome: the Chessomnia palette, navy #2B303E to blue #3193C6. */
val LogoNavy = Color(0xFF2B303E)
val LogoBlue = Color(0xFF3193C6)

val AppBackground = Color(0xFF161A22)
val AppSurface = Color(0xFF212734)
val AppSurfaceHigh = Color(0xFF2B303E)
val AppOutline = Color(0xFF3C4456)

val TextPrimary = Color(0xFFE8ECF2)
val TextSecondary = Color(0xFF9AA5B8)

/**
 * The grid is a sheet of paper on a dark desk. Deliberately independent of the app
 * chrome: a Sudoku is read for minutes at a time, and dark digits on light paper is
 * what the eye is used to from a newspaper.
 */
val PaperBase = Color(0xFFF4F1E8)
val PaperGivenCell = Color(0xFFE8E3D5)
val PaperSelected = Color(0xFFBFDCEF)

/**
 * Tint of the selected cell's row, column and box. Deliberately very weak.
 *
 * It covers 21 cells at once, so anything stronger out-shouts the same-digit
 * highlight below -- which covers at most nine and is the thing players actually
 * look for. An earlier, darker value (0xFFE2EDF4) made the board read as "a big
 * blue cross" and the matching digits disappeared inside it.
 */
val PaperPeer = Color(0xFFEDF3F8)

/** Cells holding the same digit as the selected one. Has to win against [PaperPeer]. */
val PaperSameDigit = Color(0xFFA9D98A)

val PaperConflict = Color(0xFFF2C0BC)

/**
 * The cell a hint points at. Blue, green and red are taken (selection, same digit,
 * conflict), so the hint gets amber -- and it may be strong, because it is exactly
 * one cell.
 */
val PaperHint = Color(0xFFF2C97A)

/** The unit that justifies a hint. Nine cells, so as weak as [PaperPeer]. */
val PaperHintUnit = Color(0xFFFBF0D8)

/**
 * Cells written on trial, in an open branch. Yellow -- the one hue still free next to
 * selection blue, same-digit green, conflict red and hint amber.
 *
 * The digits get [InkTrial] on top of it, because the background alone is not enough:
 * a trial cell that is selected, or that carries the highlighted digit, is painted in
 * that colour instead, and "this is only provisional" must survive that.
 */
val PaperTrial = Color(0xFFF2E27A)

val InkGiven = Color(0xFF1C222E)
val InkEntry = Color(0xFF1D6FA5)
val InkConflict = Color(0xFFA32B22)
val InkNote = Color(0xFF6B7480)

/** A digit entered in an open branch -- dark amber, never mistaken for a kept entry. */
val InkTrial = Color(0xFF8A5A00)

/** A pencil mark for the currently highlighted digit -- same green family as [PaperSameDigit]. */
val InkNoteHighlight = Color(0xFF2E6B12)
val GridLine = Color(0xFF9FA6B0)
val GridLineStrong = Color(0xFF2B303E)
