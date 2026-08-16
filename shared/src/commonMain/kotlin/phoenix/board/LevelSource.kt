package phoenix.board

/**
 * Where a GameDefinition's LevelConfigs come from. [Authored] is a fixed, finite list — used by
 * staged games and by endless games that reuse one LevelConfig forever. [Generated] produces a
 * LevelConfig procedurally for any level index, for LevelMode.GENERATED games. [Composite] is a
 * generator with specific indices overridden by hand-authored LevelConfigs — e.g. procedurally
 * generated levels with a fixed bonus level every tenth clear.
 */
sealed class LevelSource {
    data class Authored(val sequence: LevelSequence) : LevelSource()
    data class Generated(val generator: LevelGenerator) : LevelSource()
    data class Composite(val generator: LevelGenerator, val authoredOverrides: Map<Int, LevelConfig>) : LevelSource()

    fun levelAt(index: Int): LevelConfig = when (this) {
        is Authored -> sequence.levels[index]
        is Generated -> generator.levelAt(index)
        is Composite -> authoredOverrides[index] ?: generator.levelAt(index)
    }
}
