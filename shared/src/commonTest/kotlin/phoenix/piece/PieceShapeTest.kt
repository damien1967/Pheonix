package phoenix.piece

import kotlin.test.Test
import kotlin.test.assertEquals

class PieceShapeTest {

    @Test
    fun given_shape_when_rotated_then_offsetsRotateCorrectly() {
        val shape = PieceShape(offsets = listOf(CellOffset(row = 1, column = 0)))
        val rotated = shape.rotated()
        assertEquals(listOf(CellOffset(row = 0, column = -1)), rotated.offsets)
    }

    @Test
    fun given_shapeRotatedFourTimes_when_compared_then_matchesOriginal() {
        val shape = PieceShape(
            offsets = listOf(
                CellOffset(row = 0, column = 0),
                CellOffset(row = 1, column = 0),
                CellOffset(row = 1, column = 1)
            )
        )
        val rotatedFourTimes = shape.rotated().rotated().rotated().rotated()
        assertEquals(shape.offsets.toSet(), rotatedFourTimes.offsets.toSet())
    }

    @Test
    fun given_twoShapesWithSameOffsets_when_compared_then_equal() {
        val first = PieceShape(offsets = listOf(CellOffset(row = 0, column = 0)))
        val second = PieceShape(offsets = listOf(CellOffset(row = 0, column = 0)))
        assertEquals(first, second)
    }

    @Test
    fun given_singleCellShape_when_rotated_then_staysAtOrigin() {
        val shape = PieceShape(offsets = listOf(CellOffset(row = 0, column = 0)))
        val rotated = shape.rotated()
        assertEquals(listOf(CellOffset(row = 0, column = 0)), rotated.offsets)
    }
}
