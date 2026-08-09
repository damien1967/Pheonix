package phoenix.piece

sealed class PieceSlot {
    object Empty : PieceSlot()
    data class Filled(val piece: GamePiece) : PieceSlot()
}
