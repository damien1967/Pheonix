package phoenix.board

data class GameBoard(
    val rowCount: Int,
    val columnCount: Int,
    private val cells: List<List<Cell>>
) {

    fun cellAt(position: BoardPosition): Cell {
        TODO("Not yet implemented — see issue #1")
    }

    fun withCell(position: BoardPosition, newState: CellState): GameBoard {
        TODO("Not yet implemented — see issue #1")
    }

    companion object {
        fun create(
            rowCount: Int,
            columnCount: Int,
            blockedPositions: List<BoardPosition> = emptyList()
        ): GameBoard {
            TODO("Not yet implemented — see issue #1")
        }
    }
}
