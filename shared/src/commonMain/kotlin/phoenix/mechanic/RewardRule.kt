package phoenix.mechanic

interface RewardRule {
    fun rewardsEarnedAt(score: Int): RewardResult
}
