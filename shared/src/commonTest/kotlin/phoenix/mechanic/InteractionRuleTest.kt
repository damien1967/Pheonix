package phoenix.mechanic

import phoenix.board.GameBoard
import phoenix.board.GridShape
import kotlin.test.Test
import kotlin.test.assertEquals

class InteractionRuleTest {

    private val board = GameBoard.create(shape = GridShape.Rectangular(width = 1, height = 1))

    @Test
    fun given_interactionRuleWithNoDropTrigger_when_resolved_then_dropsListEmpty() {
        val rule = object : InteractionRule {
            override fun resolve(board: GameBoard) = InteractionResult(board = board, resolvedCellCount = 0)
        }

        val result = rule.resolve(board)

        assertEquals(emptyList(), result.drops)
    }

    @Test
    fun given_interactionRuleWithDropTrigger_when_resolved_then_interactionResultCarriesDropEvent() {
        val rule = object : InteractionRule {
            override fun resolve(board: GameBoard) = InteractionResult(
                board = board,
                resolvedCellCount = 4,
                drops = listOf(DropEvent(powerUpId = "line_bomb"))
            )
        }

        val result = rule.resolve(board)

        assertEquals(listOf(DropEvent(powerUpId = "line_bomb")), result.drops)
    }
}
