package phoenix.mechanic.gridfillclear

import phoenix.board.BoardPosition
import phoenix.board.CellState
import phoenix.board.GameBoard
import phoenix.mechanic.PlacementRule
import phoenix.piece.GamePiece

class GridFillClearPlacementRule : PlacementRule {

    override fun isLegalPlacement(board: GameBoard, piece: GamePiece, origin: BoardPosition): Boolean {
        return piece.shape.offsets.all { offset ->
            val row = origin.row + offset.row
            val column = origin.column + offset.column
            isWithinBounds(board, row, column) &&
                board.cellAt(BoardPosition(row = row, column = column)).state == CellState.EMPTY
        }
    }

    private fun isWithinBounds(board: GameBoard, row: Int, column: Int): Boolean {
        return row in 0 until board.rowCount && column in 0 until board.columnCount
    }
}
