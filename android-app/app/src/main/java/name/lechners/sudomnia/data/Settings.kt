package name.lechners.sudomnia.data

/**
 * The visual aids, all of which can be switched off.
 *
 * They are not decoration. Each one does part of the player's work for them, and
 * how much help a puzzle should give is a matter of taste, not a default someone
 * else gets to pick.
 *
 * @param showConflicts marks a digit that already appears in the same row, column
 *   or box. This is the big one: with it on, the grid tells you immediately whether
 *   a digit can go where you put it, which removes the entire "check before you
 *   commit" half of solving. Off means the app stays silent and you find out when
 *   the grid does not come out.
 * @param highlightSameDigit tints every cell holding the digit of the selected cell
 * @param highlightPeers tints the row, column and box of the selected cell
 * @param dimCompletedDigits greys out a keypad digit once it has been placed nine
 *   times -- a small piece of counting the app does for you
 */
data class Settings(
    val showConflicts: Boolean = true,
    val highlightSameDigit: Boolean = true,
    val highlightPeers: Boolean = true,
    val dimCompletedDigits: Boolean = true,
) {
    /** True if nothing is being given away -- drives the "no aids" marker in the UI. */
    val allAidsOff: Boolean
        get() = !showConflicts && !highlightSameDigit && !highlightPeers && !dimCompletedDigits
}
