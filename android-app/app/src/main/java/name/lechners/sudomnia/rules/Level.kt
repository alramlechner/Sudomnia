package name.lechners.sudomnia.rules

/**
 * The four difficulty bands -- **measured**, not intended.
 *
 * Each band is defined by the hardest rung of [Technique] a puzzle actually needs,
 * found by solving it the way a person would ([HumanSolver]). [Digger] aims at a band
 * while digging and [Digger.classify] measures what came out, so the label the player
 * sees cannot be a promise the puzzle does not keep.
 *
 * | Band | What it takes |
 * |---|---|
 * | [EASY] | singles only, and at least [Digger.EASY_MIN_CLUES] clues left standing |
 * | [MEDIUM] | singles only, but dug as deep as uniqueness allows |
 * | [HARD] | locked candidates or a subset (naked/hidden pair or triple) |
 * | [EXPERT] | a fish, colouring or an XY-Wing -- X-Wing and above |
 *
 * ### Every puzzle is solvable without guessing
 *
 * That is the promise this enum is built on, and it used to be broken: the old
 * "hard" band accepted anything with a unique solution, and measurement showed
 * **53 % of those could not be finished by any technique in the ladder** -- they
 * needed forcing chains or, in practice, trial and error. The digger now takes a
 * removal back if the ladder can no longer finish the puzzle, so the band is a
 * statement about the reasoning required, not merely about how few clues are left.
 *
 * ### Why the clue count still appears, but only once
 *
 * The number of clues is a poor difficulty signal in general -- maximally dug
 * singles-only puzzles and puzzles needing real techniques both sit at ~24.5 clues,
 * so it does not separate the upper bands at all. It only separates [EASY] from
 * [MEDIUM], where both fall to singles and the question is how many deductions there
 * are to make: 36 clues means fewer than half the grid is blank.
 *
 * ### Why the band is not "naked singles" vs. "hidden singles"
 *
 * That was the first attempt and it is wrong. A naked single ("this cell has only
 * one candidate left") requires scanning all 20 peers of a cell and ruling out eight
 * digits. A hidden single ("this box has only one spot left for a 5") is found by
 * scanning three lines and is what every beginner's guide teaches first.
 * Machine-cheap and human-cheap run in opposite directions here, so splitting the
 * easy tiers along that line would have labelled the *easier* puzzles harder. The
 * same argument shapes the order of [Technique] itself.
 */
enum class Level { EASY, MEDIUM, HARD, EXPERT }
