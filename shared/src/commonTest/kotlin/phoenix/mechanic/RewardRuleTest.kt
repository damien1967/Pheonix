package phoenix.mechanic

import kotlin.test.Test
import kotlin.test.assertEquals

class RewardRuleTest {

    @Test
    fun given_rewardRuleWithNoDropTrigger_when_evaluated_then_dropsListEmpty() {
        val rule = object : RewardRule {
            override fun rewardsEarnedAt(score: Int) = RewardResult(rewards = emptyList())
        }

        val result = rule.rewardsEarnedAt(score = 100)

        assertEquals(emptyList(), result.drops)
    }

    @Test
    fun given_rewardRuleWithDropTrigger_when_evaluated_then_rewardResultCarriesDropEvent() {
        val rule = object : RewardRule {
            override fun rewardsEarnedAt(score: Int) = RewardResult(
                rewards = listOf(Reward(id = "cosmetic_skin")),
                drops = listOf(DropEvent(powerUpId = "wildcard"))
            )
        }

        val result = rule.rewardsEarnedAt(score = 1000)

        assertEquals(listOf(Reward(id = "cosmetic_skin")), result.rewards)
        assertEquals(listOf(DropEvent(powerUpId = "wildcard")), result.drops)
    }
}
