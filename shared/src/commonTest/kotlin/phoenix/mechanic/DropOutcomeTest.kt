package phoenix.mechanic

import kotlin.test.Test
import kotlin.test.assertEquals

class DropOutcomeTest {

    @Test
    fun given_weightedOddsSummingBelowOneHundred_when_totalWeightPercentRead_then_shortfallIsImplicitNoDropChance() {
        val outcome = DropOutcome.Weighted(
            odds = listOf(
                WeightedDrop(powerUpId = "LineBomb", weightPercent = 5),
                WeightedDrop(powerUpId = "ScoreMultiplier", weightPercent = 10)
            )
        )

        assertEquals(15, outcome.totalWeightPercent)
    }

    @Test
    fun given_weightedOddsSummingToOneHundred_when_totalWeightPercentRead_then_noImplicitNoDropChance() {
        val outcome = DropOutcome.Weighted(
            odds = listOf(
                WeightedDrop(powerUpId = "LineBomb", weightPercent = 5),
                WeightedDrop(powerUpId = "ScoreMultiplier", weightPercent = 10),
                WeightedDrop(powerUpId = "CellEraser", weightPercent = 85)
            )
        )

        assertEquals(100, outcome.totalWeightPercent)
    }
}
