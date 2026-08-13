package phoenix.mechanic

import phoenix.board.GridShape
import phoenix.board.LevelConfig
import phoenix.board.LevelSequence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProgressionRuleTest {

    private val threeLevelSequence = LevelSequence(
        levels = listOf(
            LevelConfig(shape = GridShape.Rectangular(width = 4, height = 4)),
            LevelConfig(shape = GridShape.Rectangular(width = 5, height = 5)),
            LevelConfig(shape = GridShape.Rectangular(width = 6, height = 6))
        )
    )

    private val stagedRule = object : ProgressionRule {
        override fun speedMultiplierAtTurn(turnCount: Int) = 1.0

        override fun nextLevel(levelSequence: LevelSequence, completedLevelIndex: Int) =
            levelSequence.levels[completedLevelIndex + 1]

        override fun levelOutcome(
            sessionOutcome: SessionOutcome,
            levelSequence: LevelSequence,
            completedLevelIndex: Int
        ): LevelOutcome {
            return when (sessionOutcome) {
                SessionOutcome.Won ->
                    if (completedLevelIndex < levelSequence.levels.lastIndex) {
                        LevelOutcome.LevelCleared
                    } else {
                        LevelOutcome.FinalLevelCleared
                    }
                SessionOutcome.Lost -> LevelOutcome.GameOver
                SessionOutcome.Ongoing ->
                    throw IllegalArgumentException("levelOutcome should not be called while a session is Ongoing")
            }
        }
    }

    @Test
    fun given_wonWithMoreLevelsRemaining_when_levelOutcomeComputed_then_returnsLevelCleared() {
        val outcome = stagedRule.levelOutcome(SessionOutcome.Won, threeLevelSequence, completedLevelIndex = 0)
        assertEquals(LevelOutcome.LevelCleared, outcome)
    }

    @Test
    fun given_wonAtFinalLevel_when_levelOutcomeComputed_then_returnsFinalLevelCleared() {
        val outcome = stagedRule.levelOutcome(SessionOutcome.Won, threeLevelSequence, completedLevelIndex = 2)
        assertEquals(LevelOutcome.FinalLevelCleared, outcome)
    }

    @Test
    fun given_lostInStagedGame_when_levelOutcomeComputed_then_returnsGameOver() {
        val outcome = stagedRule.levelOutcome(SessionOutcome.Lost, threeLevelSequence, completedLevelIndex = 1)
        assertEquals(LevelOutcome.GameOver, outcome)
    }

    @Test
    fun given_ongoingSession_when_levelOutcomeComputed_then_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            stagedRule.levelOutcome(SessionOutcome.Ongoing, threeLevelSequence, completedLevelIndex = 0)
        }
    }

    @Test
    fun given_levelCleared_when_nextLevelRequested_then_returnsFollowingLevelConfig() {
        val next = stagedRule.nextLevel(threeLevelSequence, completedLevelIndex = 0)
        assertEquals(threeLevelSequence.levels[1], next)
    }
}
