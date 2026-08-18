package phoenix.mechanic.gridfillclear

import phoenix.board.BoardPosition
import phoenix.board.CellState
import phoenix.board.GameBoard
import phoenix.board.GridShape
import phoenix.mechanic.DropEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class GridFillClearInteractionRuleTest {

    private val rule = GridFillClearInteractionRule()

    private fun boardWithOccupied(rowCount: Int, columnCount: Int, occupied: List<BoardPosition>): GameBoard {
        var board = GameBoard.create(shape = GridShape.Rectangular(width = columnCount, height = rowCount))
        occupied.forEach { position -> board = board.withCell(position, CellState.OCCUPIED) }
        return board
    }

    @Test
    fun given_fullRow_when_placementResolves_then_rowClears() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2))
        )
        val result = rule.resolve(board)
        assertEquals(3, result.resolvedCellCount)
        assertEquals(1, result.resolvedGroupCount)
        for (column in 0 until 3) {
            assertEquals(CellState.EMPTY, result.board.cellAt(BoardPosition(0, column)).state)
        }
    }

    @Test
    fun given_fullColumn_when_resolved_then_columnClears() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(BoardPosition(0, 1), BoardPosition(1, 1), BoardPosition(2, 1))
        )
        val result = rule.resolve(board)
        assertEquals(3, result.resolvedCellCount)
        assertEquals(1, result.resolvedGroupCount)
        for (row in 0 until 3) {
            assertEquals(CellState.EMPTY, result.board.cellAt(BoardPosition(row, 1)).state)
        }
    }

    @Test
    fun given_simultaneousRowAndColumn_when_cleared_then_bothResolveTogether() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(
                BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2), // row 0
                BoardPosition(1, 0), BoardPosition(2, 0) // column 0, minus the shared (0,0)
            )
        )
        val result = rule.resolve(board)
        assertEquals(5, result.resolvedCellCount)
        assertEquals(2, result.resolvedGroupCount)
        assertEquals(CellState.EMPTY, result.board.cellAt(BoardPosition(0, 0)).state)
        assertEquals(CellState.EMPTY, result.board.cellAt(BoardPosition(1, 0)).state)
        assertEquals(CellState.EMPTY, result.board.cellAt(BoardPosition(2, 0)).state)
        assertEquals(CellState.EMPTY, result.board.cellAt(BoardPosition(0, 2)).state)
    }

    @Test
    fun given_multipleFullRows_when_resolved_then_allRowsClearSimultaneously() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(
                BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2),
                BoardPosition(2, 0), BoardPosition(2, 1), BoardPosition(2, 2)
            )
        )
        val result = rule.resolve(board)
        assertEquals(6, result.resolvedCellCount)
        assertEquals(2, result.resolvedGroupCount)
    }

    @Test
    fun given_noFullRowsOrColumns_when_resolved_then_boardUnchanged() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(BoardPosition(0, 0), BoardPosition(1, 1))
        )
        val result = rule.resolve(board)
        assertEquals(0, result.resolvedCellCount)
        assertEquals(0, result.resolvedGroupCount)
        assertEquals(board, result.board)
    }

    @Test
    fun given_resolve_then_originalBoardUnchanged() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2))
        )
        rule.resolve(board)
        assertEquals(CellState.OCCUPIED, board.cellAt(BoardPosition(0, 0)).state)
    }

    @Test
    fun given_noLinesCleared_when_resolved_then_dropsListEmpty() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(BoardPosition(0, 0), BoardPosition(1, 1))
        )
        val result = rule.resolve(board)
        assertEquals(emptyList(), result.drops)
    }

    @Test
    fun given_singleLineCleared_when_resolved_then_pieceSwapDropOnly() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2))
        )
        val result = rule.resolve(board)
        assertEquals(listOf(DropEvent(powerUpId = "PieceSwap")), result.drops)
    }

    @Test
    fun given_twoLinesSimultaneouslyCleared_when_resolved_then_pieceSwapAndScoreMultiplierDrop() {
        val board = boardWithOccupied(
            rowCount = 3,
            columnCount = 3,
            occupied = listOf(
                BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2),
                BoardPosition(1, 0), BoardPosition(1, 1), BoardPosition(1, 2)
            )
        )
        val result = rule.resolve(board)
        assertEquals(
            listOf(DropEvent(powerUpId = "PieceSwap"), DropEvent(powerUpId = "ScoreMultiplier")),
            result.drops
        )
    }

    @Test
    fun given_threeLinesSimultaneouslyCleared_when_resolved_then_pieceSwapCellEraserAndTrayRefreshDrop() {
        val board = boardWithOccupied(
            rowCount = 4,
            columnCount = 3,
            occupied = listOf(
                BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2),
                BoardPosition(1, 0), BoardPosition(1, 1), BoardPosition(1, 2),
                BoardPosition(2, 0), BoardPosition(2, 1), BoardPosition(2, 2)
            )
        )
        val result = rule.resolve(board)
        assertEquals(
            listOf(
                DropEvent(powerUpId = "PieceSwap"),
                DropEvent(powerUpId = "CellEraser"),
                DropEvent(powerUpId = "TrayRefresh")
            ),
            result.drops
        )
    }

    @Test
    fun given_fourLinesSimultaneouslyCleared_when_resolved_then_pieceSwapAndLineBombDrop() {
        val board = boardWithOccupied(
            rowCount = 5,
            columnCount = 4,
            occupied = listOf(
                BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2), BoardPosition(0, 3),
                BoardPosition(1, 0), BoardPosition(1, 1), BoardPosition(1, 2), BoardPosition(1, 3),
                BoardPosition(2, 0), BoardPosition(2, 1), BoardPosition(2, 2), BoardPosition(2, 3),
                BoardPosition(3, 0), BoardPosition(3, 1), BoardPosition(3, 2), BoardPosition(3, 3)
            )
        )
        val result = rule.resolve(board)
        assertEquals(
            listOf(DropEvent(powerUpId = "PieceSwap"), DropEvent(powerUpId = "LineBomb")),
            result.drops
        )
    }
}
