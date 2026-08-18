package phoenix.mechanic.gridfillclear

import phoenix.mechanic.DropEvent
import phoenix.mechanic.InteractionResult
import phoenix.mechanic.ScoringResult
import phoenix.mechanic.ScoringRule

class GridFillClearScoringRule : ScoringRule {

    override fun score(interactionResult: InteractionResult, currentScore: Int): ScoringResult {
        val lineCount = interactionResult.resolvedGroupCount
        val comboMultiplier = maxOf(1, lineCount)
        val newScore = currentScore + lineCount * LINE_CLEAR_BASE_BONUS * comboMultiplier

        return ScoringResult(
            score = newScore,
            drops = wildcardDrops(previousScore = currentScore, newScore = newScore)
        )
    }

    /**
     * PHOENIX_REWARDS_AND_AUGMENTATION.md §5.3: one Wildcard per milestone, not repeating. Stateless
     * by construction — score only rises within a session, so a threshold crossed on a prior
     * placement will always be <= currentScore on every later call and can never fire again.
     */
    private fun wildcardDrops(previousScore: Int, newScore: Int): List<DropEvent> {
        return SCORE_MILESTONES
            .filter { milestone -> previousScore < milestone && milestone <= newScore }
            .map { DropEvent(powerUpId = "Wildcard") }
    }

    companion object {
        private const val LINE_CLEAR_BASE_BONUS = 100
        private val SCORE_MILESTONES = listOf(500, 1000, 2000, 5000)
    }
}
