package phoenix.piece

sealed class PieceLifecycleState {
    object InSource : PieceLifecycleState()
    object Held : PieceLifecycleState()
    data class Preview(val isValid: Boolean) : PieceLifecycleState()
    object Placed : PieceLifecycleState()
    object Resolved : PieceLifecycleState()
    object Discarded : PieceLifecycleState()
}
