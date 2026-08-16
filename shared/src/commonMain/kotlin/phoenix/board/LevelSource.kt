package phoenix.board

/**
 * Where a GameDefinition's LevelConfigs come from. [Authored] is a fixed, finite list — used by
 * staged games and by endless games that reuse one LevelConfig forever. [Generated] produces a
 * LevelConfig procedurally for any level index, for LevelMode.GENERATED games.
 */
sealed class LevelSource {
    data class Authored(val sequence: LevelSequence) : LevelSource()
    data class Generated(val generator: LevelGenerator) : LevelSource()
}
