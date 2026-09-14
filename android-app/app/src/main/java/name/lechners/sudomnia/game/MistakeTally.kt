package name.lechners.sudomnia.game

/**
 * The three strikes of the "flag wrong entries" aid.
 *
 * A plain value with the whole rule in it, for the same reason `data/Stats.kt` is one:
 * the project has no Robolectric, so a counter that lived in the view model would be
 * untestable. Here the rule is a pure function and an ordinary JVM test covers it.
 *
 * The count only ever goes up. Undo takes the digit back, not the fact that it was
 * entered -- otherwise the aid would cost nothing at all: type, get told, undo, repeat.
 */
@JvmInline
value class MistakeTally(val count: Int = 0) {

    val lost: Boolean get() = count >= LIMIT

    /** Strikes left before the game ends; 0 once it has. */
    val remaining: Int get() = (LIMIT - count).coerceAtLeast(0)

    /**
     * @param placed what now stands in the cell -- 0 when the tap cleared it
     * @param correct the digit the solution has there
     */
    fun after(placed: Int, correct: Int): MistakeTally =
        if (placed != 0 && placed != correct) MistakeTally(count + 1) else this

    companion object {
        const val LIMIT = 3
    }
}
