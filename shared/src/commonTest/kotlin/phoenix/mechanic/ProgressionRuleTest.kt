package phoenix.mechanic

import phoenix.board.GridShape
import phoenix.board.LevelConfig
import phoenix.board.LevelSequence
import phoenix.board.LevelSource
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
    private val threeLevelSource = LevelSource.Authored(threeLevelSequence)

    private val stagedRule = object : ProgressionRule {
        override fun speedMultiplierAtTurn(turnCount: Int) = 1.0

        override fun nextLevel(levelSource: LevelSource, completedLevelIndex: Int): LevelConfig {
            check(levelSource is LevelSource.Authored)
            return levelSource.sequence.levels[completedLevelIndex + 1]
        }

        override fun levelOutcome(
            sessionOutcome: SessionOutcome,
            levelSource: LevelSource,
            completedLevelIndex: Int
        ): LevelOutcome {
            check(levelSource is LevelSource.Authored)
            return when (sessionOutcome) {
                SessionOutcome.Won ->
                    if (completedLevelIndex < levelSource.sequence.levels.lastIndex) {
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
        val outcome = stagedRule.levelOutcome(SessionOutcome.Won, threeLevelSource, completedLevelIndex = 0)
        assertEquals(LevelOutcome.LevelCleared, outcome)
    }

    @Test
    fun given_wonAtFinalLevel_when_levelOutcomeComputed_then_returnsFinalLevelCleared() {
        val outcome = stagedRule.levelOutcome(SessionOutcome.Won, threeLevelSource, completedLevelIndex = 2)
        assertEquals(LevelOutcome.FinalLevelCleared, outcome)
    }

    @Test
    fun given_lostInStagedGame_when_levelOutcomeComputed_then_returnsGameOver() {
        val outcome = stagedRule.levelOutcome(SessionOutcome.Lost, threeLevelSource, completedLevelIndex = 1)
        assertEquals(LevelOutcome.GameOver, outcome)
    }

    @Test
    fun given_ongoingSession_when_levelOutcomeComputed_then_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            stagedRule.levelOutcome(SessionOutcome.Ongoing, threeLevelSource, completedLevelIndex = 0)
        }
    }

    @Test
    fun given_levelCleared_when_nextLevelRequested_then_returnsFollowingLevelConfig() {
        val next = stagedRule.nextLevel(threeLevelSource, completedLevelIndex = 0)
        assertEquals(threeLevelSequence.levels[1], next)
    }

    @Test
    fun given_generatedLevelSource_when_nextLevelRequested_then_generatorProducesConfigForIndex() {
        val generatedRule = object : ProgressionRule {
            override fun speedMultiplierAtTurn(turnCount: Int) = 1.0

            override fun nextLevel(levelSource: LevelSource, completedLevelIndex: Int): LevelConfig {
                check(levelSource is LevelSource.Generated)
                return levelSource.generator.levelAt(completedLevelIndex + 1)
            }

            override fun levelOutcome(
                sessionOutcome: SessionOutcome,
                levelSource: LevelSource,
                completedLevelIndex: Int
            ) = LevelOutcome.LevelCleared
        }
        val generatedSource = LevelSource.Generated { index ->
            LevelConfig(shape = GridShape.Rectangular(width = 4 + index, height = 4 + index))
        }

        val next = generatedRule.nextLevel(generatedSource, completedLevelIndex = 0)

        assertEquals(GridShape.Rectangular(width = 5, height = 5), next.shape)
    }
}
