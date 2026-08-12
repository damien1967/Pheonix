package phoenix.board

data class GameBoard(
    val shape: GridShape,
    private val cells: Map<BoardPosition, Cell>
) {

    fun cellAt(position: BoardPosition): Cell {
        requireCellExists(position)
        return cells.getValue(position)
    }

    fun withCell(position: BoardPosition, newState: CellState): GameBoard {
        requireCellExists(position)
        return copy(cells = cells + (position to Cell(state = newState)))
    }

    private fun requireCellExists(position: BoardPosition) {
        require(shape.contains(position)) {
            "Position $position is absent from this board's GridShape"
        }
    }

    companion object {
        fun create(
            shape: GridShape,
            blockedPositions: List<BoardPosition> = emptyList()
        ): GameBoard {
            val blockedPositionSet = blockedPositions.toSet()
            val cells = shape.positions.associateWith { position ->
                val state = if (position in blockedPositionSet) CellState.BLOCKED else CellState.EMPTY
                Cell(state = state)
            }
            return GameBoard(shape = shape, cells = cells)
        }
    }
}
