package phoenix.mechanic

data class RewardResult(
    val rewards: List<Reward>,
    val drops: List<DropEvent> = emptyList()
)
