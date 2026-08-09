package phoenix.piece

data class GamePiece(
    val shape: PieceShape,
    val lifecycleState: PieceLifecycleState
) {

    fun rotated(): GamePiece {
        return copy(shape = shape.rotated())
    }

    fun transitionTo(newState: PieceLifecycleState): GamePiece {
        return copy(lifecycleState = newState)
    }
}
