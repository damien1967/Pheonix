package phoenix.mechanic.gridfillclear

import phoenix.board.BoardPosition
import phoenix.board.CellState
import phoenix.board.GameBoard
import phoenix.mechanic.InteractionResult
import phoenix.mechanic.InteractionRule

class GridFillClearInteractionRule : InteractionRule {

    override fun resolve(board: GameBoard): InteractionResult {
        val rowIndices = board.shape.positions.map { it.row }.distinct()
        val columnIndices = board.shape.positions.map { it.column }.distinct()

        val fullRows = rowIndices.filter { row -> isRowFull(board, row) }
        val fullColumns = columnIndices.filter { column -> isColumnFull(board, column) }

        val positionsToClear = mutableSetOf<BoardPosition>()
        board.shape.positions.forEach { position ->
            if (position.row in fullRows || position.column in fullColumns) {
                positionsToClear.add(position)
            }
        }

        var updatedBoard = board
        positionsToClear.forEach { position ->
            updatedBoard = updatedBoard.withCell(position, CellState.EMPTY)
        }

        return InteractionResult(board = updatedBoard, resolvedCellCount = positionsToClear.size)
    }

    private fun isRowFull(board: GameBoard, row: Int): Boolean {
        return board.shape.positions.filter { it.row == row }.all { position ->
            board.cellAt(position).state == CellState.OCCUPIED
        }
    }

    private fun isColumnFull(board: GameBoard, column: Int): Boolean {
        return board.shape.positions.filter { it.column == column }.all { position ->
            board.cellAt(position).state == CellState.OCCUPIED
        }
    }
}
