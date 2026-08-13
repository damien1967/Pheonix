package phoenix.mechanic

import phoenix.board.LevelConfig
import phoenix.board.LevelSequence

interface ProgressionRule {
    fun speedMultiplierAtTurn(turnCount: Int): Double

    fun nextLevel(levelSequence: LevelSequence, completedLevelIndex: Int): LevelConfig

    /** sessionOutcome must be Won or Lost — implementations throw IllegalArgumentException for Ongoing. */
    fun levelOutcome(sessionOutcome: SessionOutcome, levelSequence: LevelSequence, completedLevelIndex: Int): LevelOutcome
}
