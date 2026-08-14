package phoenix.mechanic.gridfillclear

import phoenix.board.BoardPosition
import phoenix.board.CellState
import phoenix.board.GameBoard
import phoenix.mechanic.DropEvent
import phoenix.mechanic.InteractionResult
import phoenix.mechanic.InteractionRule

class GridFillClearInteractionRule : InteractionRule {

    override fun resolve(board: GameBoard): InteractionResult {
        val rowIndices = board.shape.positions.map { it.row }.distinct()
        val columnIndices = board.shape.positions.map { it.column }.distinct()

        val fullRows = rowIndices.filter { row -> isRowFull(board, row) }
        val fullColumns = columnIndices.filter { column -> isColumnFull(board, column) }
        val lineCount = fullRows.size + fullColumns.size

        val positionsToClear = mutableSetOf<BoardPosition>()
        board.shape.positions.forEach { position ->
            if (position.row in fullRows || position.column in fullColumns) {
                positionsToClear.add(position)
            }
        }

        var updatedBoard = board
        positionsToClear.forEach { position ->
            updatedBoard = updatedBoard.withCell(position, CellState.EMPTY)
        }

        return InteractionResult(
            board = updatedBoard,
            resolvedCellCount = positionsToClear.size,
            drops = dropsForLineCount(lineCount)
        )
    }

    /**
     * PHOENIX_REWARDS_AND_AUGMENTATION.md §3.2: Piece Swap fires on any line clear, in addition
     * to whichever higher-tier drop also fires — the spec's own Cell Eraser/Tray Refresh note
     * ("both can drop from the same trigger event") establishes that triggers stack rather than
     * being mutually exclusive.
     */
    private fun dropsForLineCount(lineCount: Int): List<DropEvent> {
        if (lineCount == 0) return emptyList()

        val drops = mutableListOf(DropEvent(powerUpId = "PieceSwap"))
        when {
            lineCount == 2 -> drops.add(DropEvent(powerUpId = "ScoreMultiplier"))
            lineCount == 3 -> {
                drops.add(DropEvent(powerUpId = "CellEraser"))
                drops.add(DropEvent(powerUpId = "TrayRefresh"))
            }
            lineCount >= 4 -> drops.add(DropEvent(powerUpId = "LineBomb"))
        }
        return drops
    }

    private fun isRowFull(board: GameBoard, row: Int): Boolean {
        return board.shape.positions.filter { it.row == row }.all { position ->
            board.cellAt(position).state == CellState.OCCUPIED
        }
    }

    private fun isColumnFull(board: GameBoard, column: Int): Boolean {
        return board.shape.positions.filter { it.column == column }.all { position ->
            board.cellAt(position).state == CellState.OCCUPIED
        }
    }
}
