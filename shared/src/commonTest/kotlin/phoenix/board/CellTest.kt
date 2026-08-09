package phoenix.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CellTest {

    @Test
    fun given_cellWithEmptyState_when_queried_then_stateIsEmpty() {
        val cell = Cell(state = CellState.EMPTY)
        assertEquals(CellState.EMPTY, cell.state)
    }

    @Test
    fun given_cellWithOccupiedState_when_queried_then_stateIsOccupied() {
        val cell = Cell(state = CellState.OCCUPIED)
        assertEquals(CellState.OCCUPIED, cell.state)
    }

    @Test
    fun given_cellWithBlockedState_when_queried_then_stateIsBlocked() {
        val cell = Cell(state = CellState.BLOCKED)
        assertEquals(CellState.BLOCKED, cell.state)
    }

    @Test
    fun given_cellWithMarkedState_when_queried_then_stateIsMarked() {
        val cell = Cell(state = CellState.MARKED)
        assertEquals(CellState.MARKED, cell.state)
    }

    @Test
    fun given_twoCellsWithSameState_when_compared_then_equal() {
        val first = Cell(state = CellState.EMPTY)
        val second = Cell(state = CellState.EMPTY)
        assertEquals(first, second)
    }

    @Test
    fun given_twoCellsWithDifferentState_when_compared_then_notEqual() {
        val first = Cell(state = CellState.EMPTY)
        val second = Cell(state = CellState.OCCUPIED)
        assertNotEquals(first, second)
    }
}
