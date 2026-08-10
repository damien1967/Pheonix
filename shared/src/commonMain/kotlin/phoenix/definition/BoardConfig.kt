package phoenix.definition

import phoenix.board.BoardPosition

data class BoardConfig(
    val rowCount: Int,
    val columnCount: Int,
    val blockedPositions: List<BoardPosition> = emptyList()
)
