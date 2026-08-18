package phoenix.mechanic

import phoenix.board.GameBoard

/**
 * [resolvedGroupCount] is how many separate outcomes resolved simultaneously in this one
 * interaction step — e.g. rows/columns cleared together for a line-clear mechanic, or merge
 * clusters for a merge mechanic. `ScoringRule` uses it for combo/chain multipliers.
 */
data class InteractionResult(
    val board: GameBoard,
    val resolvedCellCount: Int,
    val resolvedGroupCount: Int = 0,
    val drops: List<DropEvent> = emptyList()
)
