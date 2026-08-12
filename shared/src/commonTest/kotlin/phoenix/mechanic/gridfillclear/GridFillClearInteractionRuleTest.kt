package phoenix.mechanic.gridfillclear

import phoenix.board.BoardPosition
import phoenix.board.CellState
import phoenix.board.GameBoard
import phoenix.board.GridShape
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
}
