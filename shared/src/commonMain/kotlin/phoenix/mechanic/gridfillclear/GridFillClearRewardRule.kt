package phoenix.mechanic.gridfillclear

import phoenix.mechanic.Reward
import phoenix.mechanic.RewardResult
import phoenix.mechanic.RewardRule

class GridFillClearRewardRule : RewardRule {

    override fun rewardsEarnedAt(score: Int): RewardResult {
        val rewards = SKIN_MILESTONES
            .filter { (threshold, _) -> score >= threshold }
            .map { (_, skinId) -> Reward(id = skinId) }

        return RewardResult(rewards = rewards)
    }

    companion object {
        private val SKIN_MILESTONES = listOf(
            1000 to "skin_bronze",
            2500 to "skin_silver",
            5000 to "skin_gold",
            10000 to "skin_platinum"
        )
    }
}
