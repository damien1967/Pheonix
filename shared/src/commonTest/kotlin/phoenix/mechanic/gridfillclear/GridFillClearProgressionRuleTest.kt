package phoenix.mechanic.gridfillclear

import phoenix.board.GridShape
import phoenix.board.LevelConfig
import phoenix.board.LevelSequence
import phoenix.mechanic.LevelOutcome
import phoenix.mechanic.SessionOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GridFillClearProgressionRuleTest {

    private val rule = GridFillClearProgressionRule()

    private val endlessLevelSequence = LevelSequence(
        levels = listOf(LevelConfig(shape = GridShape.Rectangular(width = 8, height = 8)))
    )

    @Test
    fun given_pieceResolved_when_turnAdvances_then_noTimerSideEffects() {
        assertEquals(1.0, rule.speedMultiplierAtTurn(turnCount = 0))
        assertEquals(1.0, rule.speedMultiplierAtTurn(turnCount = 1))
        assertEquals(1.0, rule.speedMultiplierAtTurn(turnCount = 500))
    }

    @Test
    fun given_lostSession_when_levelOutcomeComputed_then_returnsEndlessEnded() {
        val outcome = rule.levelOutcome(SessionOutcome.Lost, endlessLevelSequence, completedLevelIndex = 0)
        assertEquals(LevelOutcome.EndlessEnded, outcome)
    }

    @Test
    fun given_wonSession_when_levelOutcomeComputed_then_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            rule.levelOutcome(SessionOutcome.Won, endlessLevelSequence, completedLevelIndex = 0)
        }
    }

    @Test
    fun given_ongoingSession_when_levelOutcomeComputed_then_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            rule.levelOutcome(SessionOutcome.Ongoing, endlessLevelSequence, completedLevelIndex = 0)
        }
    }

    @Test
    fun given_endlessSession_when_nextLevelRequested_then_returnsSameLevelConfig() {
        val next = rule.nextLevel(endlessLevelSequence, completedLevelIndex = 0)
        assertEquals(endlessLevelSequence.levels[0], next)
    }
}
