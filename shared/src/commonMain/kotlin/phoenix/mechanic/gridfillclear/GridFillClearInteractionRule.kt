package phoenix.mechanic.gridfillclear

import phoenix.board.BoardPosition
import phoenix.board.CellState
import phoenix.board.GameBoard
import phoenix.mechanic.InteractionResult
import phoenix.mechanic.InteractionRule

class GridFillClearInteractionRule : InteractionRule {

    override fun resolve(board: GameBoard): InteractionResult {
        val fullRows = (0 until board.rowCount).filter { row -> isRowFull(board, row) }
        val fullColumns = (0 until board.columnCount).filter { column -> isColumnFull(board, column) }

        val positionsToClear = mutableSetOf<BoardPosition>()
        fullRows.forEach { row ->
            (0 until board.columnCount).forEach { column ->
                positionsToClear.add(BoardPosition(row = row, column = column))
            }
        }
        fullColumns.forEach { column ->
            (0 until board.rowCount).forEach { row ->
                positionsToClear.add(BoardPosition(row = row, column = column))
            }
        }

        var updatedBoard = board
        positionsToClear.forEach { position ->
            updatedBoard = updatedBoard.withCell(position, CellState.EMPTY)
        }

        return InteractionResult(board = updatedBoard, resolvedCellCount = positionsToClear.size)
    }

    private fun isRowFull(board: GameBoard, row: Int): Boolean {
        return (0 until board.columnCount).all { column ->
            board.cellAt(BoardPosition(row = row, column = column)).state == CellState.OCCUPIED
        }
    }

    private fun isColumnFull(board: GameBoard, column: Int): Boolean {
        return (0 until board.rowCount).all { row ->
            board.cellAt(BoardPosition(row = row, column = column)).state == CellState.OCCUPIED
        }
    }
}
