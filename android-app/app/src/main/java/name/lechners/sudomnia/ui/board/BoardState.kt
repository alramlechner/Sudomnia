package name.lechners.sudomnia.ui.board

/**
 * Everything the grid needs to draw itself, as flat arrays.
 *
 * `equals`/`hashCode` are written out by hand. A data class would compare the array
 * fields by identity, so a state that changed in place would still look "equal",
 * Compose would skip recomposition, and the board would silently freeze. That exact
 * bug already happened once in Chessomnia; it is cheap to not repeat it.
 */
class BoardState(
    val values: IntArray,
    val givens: BooleanArray,
    val notes: IntArray,
    val conflicts: BooleanArray,
    /** Cells written on trial in the open branch; all false when none is open. */
    val trial: BooleanArray,
    val selected: Int,
    /** 0 when same-digit highlighting is off or nothing is selected. */
    val highlightDigit: Int,
    /** Whether the row, column and box of the selected cell get tinted. */
    val highlightPeers: Boolean,
    /** Cell the current hint points at, or -1. */
    val hintCell: Int,
    /** Unit that justifies the current hint, or -1. */
    val hintUnit: Int,
) {
    /**
     * `conflicts` is already all-false when the player has switched conflict
     * marking off -- the board never learns the difference. Keeping the decision in
     * the view model means there is exactly one place where the app could leak the
     * answer, instead of one per drawing pass.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardState) return false
        return selected == other.selected &&
            highlightDigit == other.highlightDigit &&
            highlightPeers == other.highlightPeers &&
            hintCell == other.hintCell &&
            hintUnit == other.hintUnit &&
            values.contentEquals(other.values) &&
            givens.contentEquals(other.givens) &&
            notes.contentEquals(other.notes) &&
            conflicts.contentEquals(other.conflicts) &&
            trial.contentEquals(other.trial)
    }

    override fun hashCode(): Int {
        var h = values.contentHashCode()
        h = 31 * h + givens.contentHashCode()
        h = 31 * h + notes.contentHashCode()
        h = 31 * h + conflicts.contentHashCode()
        h = 31 * h + trial.contentHashCode()
        h = 31 * h + selected
        h = 31 * h + highlightDigit
        h = 31 * h + if (highlightPeers) 1 else 0
        h = 31 * h + hintCell
        h = 31 * h + hintUnit
        return h
    }
}
