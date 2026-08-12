package phoenix.mechanic.gridfillclear

import phoenix.board.BoardPosition
import phoenix.board.CellState
import phoenix.board.GameBoard
import phoenix.mechanic.PlacementRule
import phoenix.piece.GamePiece

class GridFillClearPlacementRule : PlacementRule {

    override fun isLegalPlacement(board: GameBoard, piece: GamePiece, origin: BoardPosition): Boolean {
        return piece.shape.offsets.all { offset ->
            val position = BoardPosition(row = origin.row + offset.row, column = origin.column + offset.column)
            board.shape.contains(position) && board.cellAt(position).state == CellState.EMPTY
        }
    }
}
