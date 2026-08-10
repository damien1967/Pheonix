package phoenix.mechanic

import phoenix.board.GameBoard

data class InteractionResult(
    val board: GameBoard,
    val resolvedCellCount: Int
)
