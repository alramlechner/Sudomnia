package name.lechners.sudomnia.rules

/**
 * Provisional difficulty bands.
 *
 * These are *not* the real grading. The real one runs a solver that only uses
 * human techniques (naked/hidden pairs, locked candidates, X-Wing, XY-Wing,
 * colouring, ...) and scores which of them were needed. That is the expensive part
 * of the full build and is not here yet. The UI says "provisional" for exactly
 * this reason.
 *
 * Until then:
 *
 *  - [HARD]   singles are not enough -- some real technique is required
 *  - [EASY]   singles suffice *and* the puzzle keeps at least [Digger.EASY_MIN_CLUES]
 *             clues, so there is little to deduce
 *  - [MEDIUM] singles suffice, but the puzzle is dug as deep as that allows
 *
 * ### Why the band is not "naked singles" vs. "hidden singles"
 *
 * That was the first attempt and it is wrong. A naked single ("this cell has only
 * one candidate left") requires scanning all 20 peers of a cell and ruling out
 * eight digits. A hidden single ("this box has only one spot left for a 5") is
 * found by scanning three lines and is what every beginner's guide teaches first.
 * Machine-cheap and human-cheap run in opposite directions here, so splitting the
 * easy tiers along that line would have labelled the *easier* puzzles harder.
 *
 * Clue count is a weak difficulty signal in general -- measurements on this
 * generator put maximally dug singles-only puzzles and puzzles needing real
 * techniques both at ~25 clues. It is only used *within* the singles-only class,
 * where it does separate "barely any deduction" from "a long chain of them".
 */
enum class Level { EASY, MEDIUM, HARD }
