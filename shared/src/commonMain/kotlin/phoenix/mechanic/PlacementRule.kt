package phoenix.mechanic

import phoenix.board.BoardPosition
import phoenix.board.GameBoard
import phoenix.piece.GamePiece

interface PlacementRule {
    fun isLegalPlacement(board: GameBoard, piece: GamePiece, origin: BoardPosition): Boolean
}
