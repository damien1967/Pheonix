package phoenix.mechanic

import phoenix.board.GameBoard
import phoenix.board.GridShape
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoringRuleTest {

    private val board = GameBoard.create(shape = GridShape.Rectangular(width = 1, height = 1))

    @Test
    fun given_scoringRuleWithNoDropTrigger_when_scored_then_dropsListEmpty() {
        val rule = object : ScoringRule {
            override fun score(interactionResult: InteractionResult, currentScore: Int) =
                ScoringResult(score = currentScore + interactionResult.resolvedCellCount)
        }

        val result = rule.score(InteractionResult(board = board, resolvedCellCount = 3), currentScore = 10)

        assertEquals(13, result.score)
        assertEquals(emptyList(), result.drops)
    }

    @Test
    fun given_scoringRuleWithDropTrigger_when_scored_then_scoringResultCarriesDropEvent() {
        val rule = object : ScoringRule {
            override fun score(interactionResult: InteractionResult, currentScore: Int) =
                ScoringResult(score = currentScore, drops = listOf(DropEvent(powerUpId = "wildcard")))
        }

        val result = rule.score(InteractionResult(board = board, resolvedCellCount = 0), currentScore = 500)

        assertEquals(listOf(DropEvent(powerUpId = "wildcard")), result.drops)
    }
}
