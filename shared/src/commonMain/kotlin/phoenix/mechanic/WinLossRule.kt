package phoenix.mechanic

import phoenix.board.GameBoard
import phoenix.piece.PieceSource

interface WinLossRule {
    fun outcome(board: GameBoard, pieceSource: PieceSource, score: Int): SessionOutcome
}
