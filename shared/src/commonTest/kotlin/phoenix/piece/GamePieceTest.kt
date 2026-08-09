package phoenix.piece

import kotlin.test.Test
import kotlin.test.assertEquals

class GamePieceTest {

    private val singleCellShape = PieceShape(offsets = listOf(CellOffset(row = 1, column = 0)))

    @Test
    fun given_gamePieceInHeldState_when_rotated_then_shapeOffsetsRotate() {
        val piece = GamePiece(shape = singleCellShape, lifecycleState = PieceLifecycleState.Held)
        val rotated = piece.rotated()
        assertEquals(singleCellShape.rotated(), rotated.shape)
    }

    @Test
    fun given_gamePiece_when_transitionedToNewState_then_lifecycleStateUpdates() {
        val piece = GamePiece(shape = singleCellShape, lifecycleState = PieceLifecycleState.InSource)
        val held = piece.transitionTo(PieceLifecycleState.Held)
        assertEquals(PieceLifecycleState.Held, held.lifecycleState)
    }

    @Test
    fun given_gamePiece_when_transitioned_then_originalPieceUnchanged() {
        val piece = GamePiece(shape = singleCellShape, lifecycleState = PieceLifecycleState.InSource)
        piece.transitionTo(PieceLifecycleState.Held)
        assertEquals(PieceLifecycleState.InSource, piece.lifecycleState)
    }

    @Test
    fun given_gamePiece_when_rotated_then_lifecycleStateUnchanged() {
        val piece = GamePiece(shape = singleCellShape, lifecycleState = PieceLifecycleState.Held)
        val rotated = piece.rotated()
        assertEquals(PieceLifecycleState.Held, rotated.lifecycleState)
    }

    @Test
    fun given_twoGamePiecesWithSameShapeAndState_when_compared_then_equal() {
        val first = GamePiece(shape = singleCellShape, lifecycleState = PieceLifecycleState.Held)
        val second = GamePiece(shape = singleCellShape, lifecycleState = PieceLifecycleState.Held)
        assertEquals(first, second)
    }

    @Test
    fun given_gamePiece_when_transitionedToInvalidPreview_then_previewStateIsInvalid() {
        val piece = GamePiece(shape = singleCellShape, lifecycleState = PieceLifecycleState.Held)
        val preview = piece.transitionTo(PieceLifecycleState.Preview(isValid = false))
        assertEquals(PieceLifecycleState.Preview(isValid = false), preview.lifecycleState)
    }
}
