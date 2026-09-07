package name.lechners.sudomnia.ui.board

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardGeometryTest {

    private val g = BoardGeometry(900f) // 100 px per cell, easy to reason about

    @Test
    fun topLeftPixelIsCellZero() {
        assertEquals(0, g.cellAt(0f, 0f))
    }

    @Test
    fun theLastPixelIsStillTheLastCell() {
        assertEquals(80, g.cellAt(899.9f, 899.9f))
    }

    @Test
    fun cellsAreFoundByRowAndColumn() {
        assertEquals(0, g.cellAt(50f, 50f))
        assertEquals(4, g.cellAt(450f, 50f))   // row 0, col 4
        assertEquals(40, g.cellAt(450f, 450f)) // centre cell
        assertEquals(72, g.cellAt(50f, 850f))  // row 8, col 0
    }

    @Test
    fun touchesOutsideTheBoardMissEverything() {
        assertEquals(-1, g.cellAt(-1f, 10f))
        assertEquals(-1, g.cellAt(10f, -1f))
        assertEquals(-1, g.cellAt(900f, 10f))
        assertEquals(-1, g.cellAt(10f, 900f))
    }

    /** Drawing and hit-testing must agree: the centre of a cell must find it back. */
    @Test
    fun everyCellCentreMapsBackToItself() {
        for (cell in 0 until 81) {
            val row = cell / 9
            val col = cell % 9
            assertEquals(cell, g.cellAt(g.centerX(col), g.centerY(row)))
        }
    }
}
