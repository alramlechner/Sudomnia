package name.lechners.sudomnia.rules

/**
 * Published puzzles with their solutions.
 *
 * The solutions were **not** produced by the code under test. They come from an
 * independent Norvig-style constraint-propagation solver run separately; checking
 * them in makes this the one place where correctness does not rest on our own
 * implementation being right.
 *
 * (The real cross-check in the full build is a Dancing Links solver in `src/test`
 * run against tens of thousands of grids. This is the prototype's stand-in.)
 */
object ReferencePuzzles {

    class Case(val name: String, val givens: String, val solution: String)

    val all = listOf(
        Case(
            "Project Euler 96, grid 1",
            "003020600900305001001806400008102900700000008006708200002609500800203009005010300",
            "483921657967345821251876493548132976729564138136798245372689514814253769695417382",
        ),
        Case(
            "17 clues, minimum possible (McGuire et al. 2012: 16 is impossible)",
            "4.....8.5.3..........7......2.....6.....8.4......1.......6.3.7.5..2.....1.4......",
            "417369825632158947958724316825437169791586432346912758289643571573291684164875293",
        ),
        Case(
            "AI Escargot (Arto Inkala)",
            "1....7.9..3..2...8..96..5....53..9...1..8...26....4...3......1..4......7..7...3..",
            "162857493534129678789643521475312986913586742628794135356478219241935867897261354",
        ),
    )
}
