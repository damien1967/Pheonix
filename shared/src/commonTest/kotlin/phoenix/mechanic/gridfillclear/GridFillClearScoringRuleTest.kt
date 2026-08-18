package phoenix.mechanic.gridfillclear

import phoenix.board.GameBoard
import phoenix.board.GridShape
import phoenix.mechanic.DropEvent
import phoenix.mechanic.InteractionResult
import kotlin.test.Test
import kotlin.test.assertEquals

class GridFillClearScoringRuleTest {

    private val rule = GridFillClearScoringRule()
    private val board = GameBoard.create(shape = GridShape.Rectangular(width = 1, height = 1))

    private fun interactionResult(resolvedGroupCount: Int) =
        InteractionResult(board = board, resolvedCellCount = 0, resolvedGroupCount = resolvedGroupCount)

    @Test
    fun given_noLineClear_when_scored_then_scoreUnchanged() {
        val result = rule.score(interactionResult(resolvedGroupCount = 0), currentScore = 200)

        assertEquals(200, result.score)
        assertEquals(emptyList(), result.drops)
    }

    @Test
    fun given_singleLineClear_when_scored_then_baseBonusApplied() {
        val result = rule.score(interactionResult(resolvedGroupCount = 1), currentScore = 0)

        assertEquals(100, result.score)
    }

    @Test
    fun given_multiLineClear_when_scored_then_comboMultiplierApplied() {
        val twoLines = rule.score(interactionResult(resolvedGroupCount = 2), currentScore = 0)
        val threeLines = rule.score(interactionResult(resolvedGroupCount = 3), currentScore = 0)
        val fourLines = rule.score(interactionResult(resolvedGroupCount = 4), currentScore = 0)

        assertEquals(400, twoLines.score)
        assertEquals(900, threeLines.score)
        assertEquals(1600, fourLines.score)
    }

    @Test
    fun given_scoreCrossesMilestone_when_scored_then_wildcardDropEmitted() {
        val result = rule.score(interactionResult(resolvedGroupCount = 1), currentScore = 450)

        assertEquals(550, result.score)
        assertEquals(listOf(DropEvent(powerUpId = "Wildcard")), result.drops)
    }

    @Test
    fun given_scoreAlreadyPastMilestone_when_scored_then_noRepeatWildcardDrop() {
        val result = rule.score(interactionResult(resolvedGroupCount = 0), currentScore = 600)

        assertEquals(600, result.score)
        assertEquals(emptyList(), result.drops)
    }

    @Test
    fun given_scoreCrossesMultipleMilestonesAtOnce_when_scored_then_multipleWildcardDropsEmitted() {
        val result = rule.score(interactionResult(resolvedGroupCount = 4), currentScore = 400)

        assertEquals(2000, result.score)
        assertEquals(
            listOf(
                DropEvent(powerUpId = "Wildcard"),
                DropEvent(powerUpId = "Wildcard"),
                DropEvent(powerUpId = "Wildcard")
            ),
            result.drops
        )
    }

    @Test
    fun given_scoreExactlyOnMilestone_when_scored_then_wildcardDropEmitted() {
        val result = rule.score(interactionResult(resolvedGroupCount = 1), currentScore = 400)

        assertEquals(500, result.score)
        assertEquals(listOf(DropEvent(powerUpId = "Wildcard")), result.drops)
    }
}
