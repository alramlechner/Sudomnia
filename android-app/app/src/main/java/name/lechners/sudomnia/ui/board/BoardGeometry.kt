package name.lechners.sudomnia.ui.board

/**
 * Maps between pixels and cell indices.
 *
 * Drawing and hit-testing go through the same object on purpose: when they are
 * derived separately they drift, and taps land one cell off near the edges.
 *
 * Pure arithmetic, no Android types -- so it is unit-testable.
 */
class BoardGeometry(val sizePx: Float) {

    val cellSize: Float = sizePx / 9f

    fun left(col: Int): Float = col * cellSize
    fun top(row: Int): Float = row * cellSize

    fun centerX(col: Int): Float = (col + 0.5f) * cellSize
    fun centerY(row: Int): Float = (row + 0.5f) * cellSize

    /** The cell under a touch point, or -1 outside the board. */
    fun cellAt(x: Float, y: Float): Int {
        if (x < 0f || y < 0f || x >= sizePx || y >= sizePx) return -1
        val col = (x / cellSize).toInt().coerceIn(0, 8)
        val row = (y / cellSize).toInt().coerceIn(0, 8)
        return row * 9 + col
    }
}
