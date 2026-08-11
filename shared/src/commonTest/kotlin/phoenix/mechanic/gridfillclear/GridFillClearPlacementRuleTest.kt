package phoenix.mechanic.gridfillclear

import phoenix.board.BoardPosition
import phoenix.board.CellState
import phoenix.board.GameBoard
import phoenix.piece.CellOffset
import phoenix.piece.GamePiece
import phoenix.piece.PieceLifecycleState
import phoenix.piece.PieceShape
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GridFillClearPlacementRuleTest {

    private val rule = GridFillClearPlacementRule()

    private val singleCellPiece = GamePiece(
        shape = PieceShape(offsets = listOf(CellOffset(row = 0, column = 0))),
        lifecycleState = PieceLifecycleState.Held
    )

    private val twoCellHorizontalPiece = GamePiece(
        shape = PieceShape(offsets = listOf(CellOffset(row = 0, column = 0), CellOffset(row = 0, column = 1))),
        lifecycleState = PieceLifecycleState.Held
    )

    @Test
    fun given_emptyBoardAndValidOrigin_when_validated_then_placementValid() {
        val board = GameBoard.create(rowCount = 3, columnCount = 3)
        assertTrue(rule.isLegalPlacement(board, singleCellPiece, BoardPosition(row = 1, column = 1)))
    }

    @Test
    fun given_overlapWithOccupiedCell_when_validated_then_placementInvalid() {
        val board = GameBoard.create(rowCount = 3, columnCount = 3)
            .withCell(BoardPosition(row = 1, column = 1), CellState.OCCUPIED)
        assertFalse(rule.isLegalPlacement(board, singleCellPiece, BoardPosition(row = 1, column = 1)))
    }

    @Test
    fun given_overlapWithBlockedCell_when_validated_then_placementInvalid() {
        val board = GameBoard.create(
            rowCount = 3,
            columnCount = 3,
            blockedPositions = listOf(BoardPosition(row = 1, column = 1))
        )
        assertFalse(rule.isLegalPlacement(board, singleCellPiece, BoardPosition(row = 1, column = 1)))
    }

    @Test
    fun given_pieceOffBoardEdge_when_validated_then_placementInvalid() {
        val board = GameBoard.create(rowCount = 3, columnCount = 3)
        assertFalse(rule.isLegalPlacement(board, singleCellPiece, BoardPosition(row = 3, column = 0)))
    }

    @Test
    fun given_multiCellPiecePartiallyOffBoard_when_validated_then_placementInvalid() {
        val board = GameBoard.create(rowCount = 3, columnCount = 3)
        assertFalse(rule.isLegalPlacement(board, twoCellHorizontalPiece, BoardPosition(row = 0, column = 2)))
    }
}
