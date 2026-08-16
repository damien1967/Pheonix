package phoenix.mechanic

/**
 * What a DropTrigger awards once its DropCondition fires.
 *
 * [Deterministic] emits every listed power-up, every time — for triggers the spec says should
 * always reward the same way. [Weighted] rolls once against [WeightedDrop.weightPercent] odds and
 * emits at most one power-up; odds need not sum to 100, and the shortfall is an implicit chance of
 * no drop. Weighted outcomes are for conditions that are already hard to achieve, so the reward
 * stays a rare bonus rather than a guarantee.
 */
sealed class DropOutcome {
    data class Deterministic(val powerUpIds: List<String>) : DropOutcome()

    data class Weighted(val odds: List<WeightedDrop>) : DropOutcome() {
        val totalWeightPercent: Int get() = odds.sumOf { it.weightPercent }
    }
}

data class WeightedDrop(val powerUpId: String, val weightPercent: Int)
