package phoenix.board

sealed class GridShape {

    abstract val positions: Set<BoardPosition>

    fun contains(position: BoardPosition): Boolean {
        return position in positions
    }

    data class Rectangular(val width: Int, val height: Int) : GridShape() {
        override val positions: Set<BoardPosition> = buildSet {
            for (row in 0 until height) {
                for (column in 0 until width) {
                    add(BoardPosition(row = row, column = column))
                }
            }
        }
    }

    data class CellSet(override val positions: Set<BoardPosition>) : GridShape()
}
