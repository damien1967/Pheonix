package phoenix.board

data class LevelConfig(
    val shape: GridShape,
    val blockedCells: List<BoardPosition> = emptyList()
)
