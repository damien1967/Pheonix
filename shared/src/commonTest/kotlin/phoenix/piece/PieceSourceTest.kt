package phoenix.piece

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PieceSourceTest {

    private val samplePiece = GamePiece(
        shape = PieceShape(offsets = listOf(CellOffset(row = 0, column = 0))),
        lifecycleState = PieceLifecycleState.InSource
    )

    @Test
    fun given_pieceSourceCreated_when_queried_then_allSlotsEmpty() {
        val source = PieceSource.create(slotCount = 3)
        for (index in 0 until 3) {
            assertEquals(PieceSlot.Empty, source.slotAt(index))
        }
    }

    @Test
    fun given_pieceSource_when_slotFilled_then_slotCountIncreases() {
        val source = PieceSource.create(slotCount = 3)
        val filled = source.withSlotFilled(index = 0, piece = samplePiece)
        assertEquals(1, filled.filledSlotCount)
    }

    @Test
    fun given_pieceSource_when_slotFilled_then_originalSourceUnchanged() {
        val source = PieceSource.create(slotCount = 3)
        source.withSlotFilled(index = 0, piece = samplePiece)
        assertEquals(0, source.filledSlotCount)
    }

    @Test
    fun given_pieceSource_when_slotEmptied_then_slotCountDecreases() {
        val source = PieceSource.create(slotCount = 3).withSlotFilled(index = 0, piece = samplePiece)
        val emptied = source.withSlotEmptied(index = 0)
        assertEquals(0, emptied.filledSlotCount)
    }

    @Test
    fun given_positionOutOfBounds_when_slotFilled_then_throwsIllegalArgumentException() {
        val source = PieceSource.create(slotCount = 3)
        assertFailsWith<IllegalArgumentException> {
            source.withSlotFilled(index = 9, piece = samplePiece)
        }
    }

    @Test
    fun given_positionOutOfBounds_when_slotQueried_then_throwsIllegalArgumentException() {
        val source = PieceSource.create(slotCount = 3)
        assertFailsWith<IllegalArgumentException> {
            source.slotAt(9)
        }
    }

    @Test
    fun given_pieceSource_when_slotFilled_then_neighboringSlotsUnaffected() {
        val source = PieceSource.create(slotCount = 3)
        val filled = source.withSlotFilled(index = 0, piece = samplePiece)
        assertEquals(PieceSlot.Empty, filled.slotAt(1))
        assertEquals(PieceSlot.Empty, filled.slotAt(2))
    }
}
