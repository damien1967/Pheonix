package phoenix.mechanic

import phoenix.board.LevelConfig
import phoenix.board.LevelSource

interface ProgressionRule {
    fun speedMultiplierAtTurn(turnCount: Int): Double

    fun nextLevel(levelSource: LevelSource, completedLevelIndex: Int): LevelConfig

    /** sessionOutcome must be Won or Lost — implementations throw IllegalArgumentException for Ongoing. */
    fun levelOutcome(sessionOutcome: SessionOutcome, levelSource: LevelSource, completedLevelIndex: Int): LevelOutcome
}
