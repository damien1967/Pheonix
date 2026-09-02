package phoenix.mechanic.gridfillclear

import phoenix.mechanic.Reward
import kotlin.test.Test
import kotlin.test.assertEquals

class GridFillClearRewardRuleTest {

    private val rule = GridFillClearRewardRule()

    @Test
    fun given_scoreBelowFirstThreshold_when_checked_then_noRewardGranted() {
        val result = rule.rewardsEarnedAt(score = 500)

        assertEquals(emptyList(), result.rewards)
        assertEquals(emptyList(), result.drops)
    }

    @Test
    fun given_scoreThresholdReached_when_checked_then_rewardGranted() {
        val result = rule.rewardsEarnedAt(score = 1000)

        assertEquals(listOf(Reward(id = "skin_bronze")), result.rewards)
    }

    @Test
    fun given_scoreExactlyOnThreshold_when_checked_then_rewardGranted() {
        val result = rule.rewardsEarnedAt(score = 2500)

        assertEquals(true, result.rewards.contains(Reward(id = "skin_silver")))
    }

    @Test
    fun given_scoreCrossesMultipleThresholds_when_checked_then_allEarnedRewardsReturned() {
        val result = rule.rewardsEarnedAt(score = 10000)

        assertEquals(
            listOf(
                Reward(id = "skin_bronze"),
                Reward(id = "skin_silver"),
                Reward(id = "skin_gold"),
                Reward(id = "skin_platinum")
            ),
            result.rewards
        )
    }

    @Test
    fun given_scoreBetweenThresholds_when_checkedTwice_then_sameRewardsReturnedEachCall() {
        val first = rule.rewardsEarnedAt(score = 3000)
        val second = rule.rewardsEarnedAt(score = 3000)

        assertEquals(first.rewards, second.rewards)
    }
}
