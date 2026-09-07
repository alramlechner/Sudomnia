package name.lechners.sudomnia.game

/**
 * One cell touched by one player action.
 *
 * Both the digit and the pencil-mark mask are recorded before and after, because a
 * single action changes both: placing a 5 also wipes the 5 from the notes of all 20
 * peers.
 */
class CellChange(
    val cell: Int,
    val digitBefore: Int,
    val digitAfter: Int,
    val notesBefore: Int,
    val notesAfter: Int,
)

/**
 * One player action, which may touch several cells at once.
 *
 * Undo replays the changes backwards, redo forwards. Storing the action rather
 * than a copy of the board is the same choice Chessomnia makes with its move list:
 * it keeps undo correct by construction, and it is what a saved game will be
 * serialised from once persistence arrives.
 */
class Edit(val changes: List<CellChange>)
