package phoenix.mechanic.gridfillclear

import phoenix.board.LevelConfig
import phoenix.board.LevelSource
import phoenix.mechanic.LevelOutcome
import phoenix.mechanic.ProgressionRule
import phoenix.mechanic.SessionOutcome

class GridFillClearProgressionRule : ProgressionRule {

    override fun speedMultiplierAtTurn(turnCount: Int): Double = 1.0

    override fun nextLevel(levelSource: LevelSource, completedLevelIndex: Int): LevelConfig {
        return levelSource.levelAt(0)
    }

    override fun levelOutcome(
        sessionOutcome: SessionOutcome,
        levelSource: LevelSource,
        completedLevelIndex: Int
    ): LevelOutcome {
        return when (sessionOutcome) {
            SessionOutcome.Lost -> LevelOutcome.EndlessEnded
            SessionOutcome.Won -> throw IllegalArgumentException("Grid-Fill Clear has no win condition")
            SessionOutcome.Ongoing ->
                throw IllegalArgumentException("levelOutcome should not be called while a session is Ongoing")
        }
    }
}
