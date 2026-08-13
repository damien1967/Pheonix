package phoenix.definition

import phoenix.board.BoardPosition
import phoenix.board.GridShape

data class LevelConfig(
    val shape: GridShape,
    val blockedCells: List<BoardPosition> = emptyList()
)
