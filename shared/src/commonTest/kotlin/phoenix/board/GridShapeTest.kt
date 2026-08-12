package phoenix.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GridShapeTest {

    @Test
    fun given_rectangularShape_when_queried_then_containsAllCellsWithinBounds() {
        val shape = GridShape.Rectangular(width = 3, height = 2)
        assertTrue(shape.contains(BoardPosition(row = 0, column = 0)))
        assertTrue(shape.contains(BoardPosition(row = 1, column = 2)))
    }

    @Test
    fun given_rectangularShape_when_queried_then_excludesCellsOutsideBounds() {
        val shape = GridShape.Rectangular(width = 3, height = 2)
        assertFalse(shape.contains(BoardPosition(row = 2, column = 0)))
        assertFalse(shape.contains(BoardPosition(row = 0, column = 3)))
        assertFalse(shape.contains(BoardPosition(row = -1, column = 0)))
    }

    @Test
    fun given_cellSetShape_when_queried_then_containsOnlySpecifiedCells() {
        val shape = GridShape.CellSet(
            positions = setOf(BoardPosition(row = 0, column = 0), BoardPosition(row = 5, column = 5))
        )
        assertTrue(shape.contains(BoardPosition(row = 0, column = 0)))
        assertTrue(shape.contains(BoardPosition(row = 5, column = 5)))
        assertFalse(shape.contains(BoardPosition(row = 0, column = 1)))
    }

    @Test
    fun given_rectangularShape_when_positionsEnumerated_then_countMatchesWidthTimesHeight() {
        val shape = GridShape.Rectangular(width = 4, height = 3)
        assertEquals(12, shape.positions.size)
    }

    @Test
    fun given_twoRectangularShapesWithSameDimensions_when_compared_then_equal() {
        val first = GridShape.Rectangular(width = 3, height = 3)
        val second = GridShape.Rectangular(width = 3, height = 3)
        assertEquals(first, second)
    }
}
