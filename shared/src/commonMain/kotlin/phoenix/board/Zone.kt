package phoenix.board

data class Zone(
    val name: String,
    val positions: Set<BoardPosition>
)
