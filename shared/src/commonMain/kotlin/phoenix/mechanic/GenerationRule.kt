package phoenix.mechanic

import phoenix.piece.GamePiece
import phoenix.piece.PieceSource

interface GenerationRule {
    fun nextPiece(pieceSource: PieceSource): GamePiece
}
