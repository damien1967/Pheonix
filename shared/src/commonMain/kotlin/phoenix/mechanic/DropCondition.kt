package phoenix.mechanic

/**
 * The event a DropTrigger watches for.
 *
 * Split by which layer can evaluate it. [Engine] conditions are decided from this session's own
 * board/score alone, so an InteractionRule or ScoringRule can check them directly. [Shell]
 * conditions need data the engine never holds — CLAUDE.md rule 8 keeps player history and
 * cross-session state out of the engine, and PHOENIX_REWARDS_AND_AUGMENTATION.md §8.4-8.6 place
 * that data (SessionResult, player history) in the Shell — so only the Shell can resolve them.
 * A generic engine-side trigger resolver must only ever accept [Engine] conditions; this split
 * makes that a type error to get wrong rather than a runtime bug.
 *
 * Within [Engine]: [AnyLineCleared] and [SimultaneousClears] are cyclic — stateless per-placement
 * checks that can fire any number of times in a session. [ScoreMilestones] is sequential: the
 * ordered thresholds are one trigger, and each threshold fires at most once per session, in order.
 */
sealed class DropCondition {

    sealed class Engine : DropCondition() {
        object AnyLineCleared : Engine()
        data class SimultaneousClears(val lineCount: Int) : Engine()
        data class ScoreMilestones(val thresholds: List<Int>) : Engine()
    }

    sealed class Shell : DropCondition() {
        /**
         * Fires when the player beats their all-time best for this GameDefinition, set in a
         * previous session. Not the same as a new high point within the current session — score
         * only rises within a session, so that would fire almost every placement. Only the Shell
         * holds cross-session player history, so only the Shell can evaluate this.
         */
        object PersonalBestInSession : Shell()
    }
}
