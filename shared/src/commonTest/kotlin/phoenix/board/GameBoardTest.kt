package phoenix.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameBoardTest {

    @Test
    fun given_boardWithDimensions_when_created_then_shapeMatchesConfig() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 8, height = 8))
        assertEquals(GridShape.Rectangular(width = 8, height = 8), board.shape)
    }

    @Test
    fun given_newBoard_when_created_then_allCellsEmpty() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 3, height = 3))
        for (row in 0 until 3) {
            for (column in 0 until 3) {
                assertEquals(CellState.EMPTY, board.cellAt(BoardPosition(row, column)).state)
            }
        }
    }

    @Test
    fun given_boardWithBlockedCells_when_created_then_specifiedCellsAreBlocked() {
        val blockedPosition = BoardPosition(row = 1, column = 1)
        val board = GameBoard.create(
            shape = GridShape.Rectangular(width = 3, height = 3),
            blockedPositions = listOf(blockedPosition)
        )
        assertEquals(CellState.BLOCKED, board.cellAt(blockedPosition).state)
    }

    @Test
    fun given_board_when_cellQueriedAtValidPosition_then_returnsCorrectCell() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 2, height = 2))
        val cell = board.cellAt(BoardPosition(row = 0, column = 1))
        assertEquals(CellState.EMPTY, cell.state)
    }

    @Test
    fun given_positionOutOfBounds_when_queried_then_throwsIllegalArgumentException() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 2, height = 2))
        assertFailsWith<IllegalArgumentException> {
            board.cellAt(BoardPosition(row = 5, column = 5))
        }
    }

    @Test
    fun given_board_when_cellSet_then_returnsNewBoardWithUpdatedCell() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 2, height = 2))
        val position = BoardPosition(row = 0, column = 0)
        val updatedBoard = board.withCell(position, CellState.OCCUPIED)
        assertEquals(CellState.OCCUPIED, updatedBoard.cellAt(position).state)
    }

    @Test
    fun given_board_when_cellSet_then_originalBoardUnchanged() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 2, height = 2))
        val position = BoardPosition(row = 0, column = 0)
        board.withCell(position, CellState.OCCUPIED)
        assertEquals(CellState.EMPTY, board.cellAt(position).state)
    }

    @Test
    fun given_cellMutated_when_neighboringCellQueried_then_unaffected() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 2, height = 2))
        val mutatedPosition = BoardPosition(row = 0, column = 0)
        val neighborPosition = BoardPosition(row = 0, column = 1)
        val updatedBoard = board.withCell(mutatedPosition, CellState.OCCUPIED)
        assertEquals(CellState.EMPTY, updatedBoard.cellAt(neighborPosition).state)
    }

    @Test
    fun given_positionOutOfBounds_when_cellSet_then_throwsIllegalArgumentException() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 2, height = 2))
        assertFailsWith<IllegalArgumentException> {
            board.withCell(BoardPosition(row = 9, column = 9), CellState.OCCUPIED)
        }
    }

    @Test
    fun given_cellSetShapedBoard_when_created_then_onlySpecifiedCellsExist() {
        val shape = GridShape.CellSet(
            positions = setOf(BoardPosition(row = 0, column = 0), BoardPosition(row = 5, column = 5))
        )
        val board = GameBoard.create(shape = shape)
        assertEquals(CellState.EMPTY, board.cellAt(BoardPosition(row = 0, column = 0)).state)
        assertEquals(CellState.EMPTY, board.cellAt(BoardPosition(row = 5, column = 5)).state)
    }

    @Test
    fun given_positionOutsideCellSetShape_when_queried_then_throwsIllegalArgumentException() {
        val shape = GridShape.CellSet(positions = setOf(BoardPosition(row = 0, column = 0)))
        val board = GameBoard.create(shape = shape)
        assertFailsWith<IllegalArgumentException> {
            board.cellAt(BoardPosition(row = 0, column = 1))
        }
    }
}
