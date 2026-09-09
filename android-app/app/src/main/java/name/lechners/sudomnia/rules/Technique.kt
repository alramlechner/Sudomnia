package name.lechners.sudomnia.rules

/**
 * The solving techniques the app can find *and explain*, ordered the way a person
 * learns them.
 *
 * [score] is the price of one application, and the ordering of the enum is the order
 * the solver tries them in: always the cheapest thing a human would spot first. The
 * numbers are relative and only have to be monotone -- they exist so that a puzzle
 * needing twenty locked-candidate steps can be told apart from one needing three,
 * which the hardest technique alone cannot say.
 *
 * ### Why hidden single comes before naked single
 *
 * For the machine a naked single is cheaper: look at one cell, count the bits. For a
 * person it is the opposite -- "this cell has only one candidate left" means ruling
 * out eight digits across twenty peers, while "in this box only one square can take
 * the 5" is a scan of three lines and the first thing every beginner's guide teaches.
 * Machine-cheap and human-cheap run in opposite directions here, and this ladder is
 * about people.
 *
 * ### The order of the top four
 *
 * X-Wing, Swordfish, colouring, XY-Wing -- that is roughly how published raters order
 * them, and it matters here for a reason beyond taste: the solver takes the *first*
 * technique that bites, so the order decides which one a puzzle is credited with.
 * Putting the wing above the fish, as an earlier draft did, meant a Swordfish was
 * almost never reached: an XY-Wing was found first nearly every time.
 *
 * ### What is deliberately missing
 *
 * Forcing chains, nice loops, ALS. They solve more puzzles, but the explanation for
 * one is a paragraph, not a sentence -- and a hint nobody can follow is worse than an
 * honest "nothing forced here". A puzzle this ladder cannot finish is not published
 * as a puzzle: [Digger] refuses to hand one out.
 *
 * Uniqueness-based techniques (unique rectangle) are missing for a different reason:
 * they argue from "the puzzle has one solution", which is a fact about the setter,
 * not about the grid. They would also make the solver useless for *checking*
 * uniqueness, which is what the generator uses it for.
 */
enum class Technique(val score: Int) {
    HIDDEN_SINGLE(1),
    NAKED_SINGLE(2),
    LOCKED_CANDIDATES(4),
    NAKED_PAIR(6),
    HIDDEN_PAIR(8),
    NAKED_TRIPLE(10),
    HIDDEN_TRIPLE(12),
    X_WING(16),
    SWORDFISH(18),
    SIMPLE_COLOURING(20),
    XY_WING(22);

    /** True for the two techniques that write a digit; all others only cross out. */
    val places: Boolean get() = this == HIDDEN_SINGLE || this == NAKED_SINGLE
}

/** One candidate struck out: digit [digit] can no longer go into [cell]. */
class Elimination(val cell: Int, val digit: Int)

/**
 * One deduction, with everything needed to *show* it: what it concludes, which cells
 * form the pattern, and which units justify it.
 *
 * The pattern and units are not decoration. A hint that says "naked pair" without
 * pointing at the two cells is a vocabulary lesson, not a hint -- and the board can
 * only highlight what this object names.
 */
class Step(
    val technique: Technique,
    /** The cell a digit goes into, or -1 for a step that only strikes candidates. */
    val cell: Int = -1,
    /** The digit placed, or the digit the pattern is about. */
    val digit: Int = 0,
    /** The cells the argument is made of -- the pair, the triple, the fish corners. */
    val pattern: IntArray = EMPTY,
    /** Row/column/box indices that carry the argument, in [Units] numbering. */
    val units: IntArray = EMPTY,
    /** For subsets: the mask of digits involved. 0 when a single digit is meant. */
    val digitMask: Int = 0,
    val eliminations: List<Elimination> = emptyList(),
) {
    companion object {
        private val EMPTY = IntArray(0)
    }
}
