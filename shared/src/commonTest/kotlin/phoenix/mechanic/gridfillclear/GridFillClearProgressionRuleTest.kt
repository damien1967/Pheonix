package phoenix.mechanic.gridfillclear

import phoenix.board.GridShape
import phoenix.board.LevelConfig
import phoenix.board.LevelGenerator
import phoenix.board.LevelSequence
import phoenix.board.LevelSource
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
    private val endlessLevelSource = LevelSource.Authored(endlessLevelSequence)

    @Test
    fun given_pieceResolved_when_turnAdvances_then_noTimerSideEffects() {
        assertEquals(1.0, rule.speedMultiplierAtTurn(turnCount = 0))
        assertEquals(1.0, rule.speedMultiplierAtTurn(turnCount = 1))
        assertEquals(1.0, rule.speedMultiplierAtTurn(turnCount = 500))
    }

    @Test
    fun given_lostSession_when_levelOutcomeComputed_then_returnsEndlessEnded() {
        val outcome = rule.levelOutcome(SessionOutcome.Lost, endlessLevelSource, completedLevelIndex = 0)
        assertEquals(LevelOutcome.EndlessEnded, outcome)
    }

    @Test
    fun given_wonSession_when_levelOutcomeComputed_then_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            rule.levelOutcome(SessionOutcome.Won, endlessLevelSource, completedLevelIndex = 0)
        }
    }

    @Test
    fun given_ongoingSession_when_levelOutcomeComputed_then_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            rule.levelOutcome(SessionOutcome.Ongoing, endlessLevelSource, completedLevelIndex = 0)
        }
    }

    @Test
    fun given_endlessSession_when_nextLevelRequested_then_returnsSameLevelConfig() {
        val next = rule.nextLevel(endlessLevelSource, completedLevelIndex = 0)
        assertEquals(endlessLevelSequence.levels[0], next)
    }

    @Test
    fun given_generatedLevelSource_when_nextLevelRequested_then_returnsGeneratorLevelZero() {
        val generatedConfig = LevelConfig(shape = GridShape.Rectangular(width = 10, height = 10))
        val generatedSource = LevelSource.Generated { index -> if (index == 0) generatedConfig else error("unexpected index") }

        val next = rule.nextLevel(generatedSource, completedLevelIndex = 0)

        assertEquals(generatedConfig, next)
    }

    @Test
    fun given_compositeLevelSourceWithOverrideAtZero_when_nextLevelRequested_then_returnsAuthoredOverride() {
        val bonusConfig = LevelConfig(shape = GridShape.Rectangular(width = 12, height = 12))
        val compositeSource = LevelSource.Composite(
            generator = LevelGenerator { index -> LevelConfig(shape = GridShape.Rectangular(width = index, height = index)) },
            authoredOverrides = mapOf(0 to bonusConfig)
        )

        val next = rule.nextLevel(compositeSource, completedLevelIndex = 0)

        assertEquals(bonusConfig, next)
    }
}
