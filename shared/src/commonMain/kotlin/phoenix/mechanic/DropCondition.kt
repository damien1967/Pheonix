package phoenix.mechanic

/**
 * The event a DropTrigger watches for.
 *
 * [AnyLineCleared], [SimultaneousClears], and [PersonalBestInSession] are cyclic: each is a
 * stateless per-placement check that can fire any number of times in a session. [ScoreMilestones]
 * is sequential: the ordered thresholds are one trigger, and each threshold fires at most once
 * per session, in order.
 */
sealed class DropCondition {
    object AnyLineCleared : DropCondition()
    data class SimultaneousClears(val lineCount: Int) : DropCondition()
    object PersonalBestInSession : DropCondition()
    data class ScoreMilestones(val thresholds: List<Int>) : DropCondition()
}
