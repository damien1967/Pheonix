package phoenix.mechanic

interface RewardRule {
    fun rewardsEarnedAt(score: Int): List<Reward>
}
