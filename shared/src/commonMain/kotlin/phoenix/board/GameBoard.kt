package phoenix.board

data class GameBoard(
    val rowCount: Int,
    val columnCount: Int,
    private val cells: List<List<Cell>>
) {

    fun cellAt(position: BoardPosition): Cell {
        requireInBounds(position)
        return cells[position.row][position.column]
    }

    fun withCell(position: BoardPosition, newState: CellState): GameBoard {
        requireInBounds(position)
        val updatedCells = cells.mapIndexed { rowIndex, row ->
            if (rowIndex != position.row) {
                row
            } else {
                row.mapIndexed { columnIndex, cell ->
                    if (columnIndex != position.column) cell else Cell(state = newState)
                }
            }
        }
        return copy(cells = updatedCells)
    }

    private fun requireInBounds(position: BoardPosition) {
        require(position.row in 0 until rowCount && position.column in 0 until columnCount) {
            "Position $position is out of bounds for a ${rowCount}x${columnCount} board"
        }
    }

    companion object {
        fun create(
            rowCount: Int,
            columnCount: Int,
            blockedPositions: List<BoardPosition> = emptyList()
        ): GameBoard {
            val blockedPositionSet = blockedPositions.toSet()
            val cells = (0 until rowCount).map { row ->
                (0 until columnCount).map { column ->
                    val state = if (BoardPosition(row, column) in blockedPositionSet) {
                        CellState.BLOCKED
                    } else {
                        CellState.EMPTY
                    }
                    Cell(state = state)
                }
            }
            return GameBoard(rowCount = rowCount, columnCount = columnCount, cells = cells)
        }
    }
}
