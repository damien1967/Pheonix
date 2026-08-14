package phoenix.mechanic

data class ScoringResult(
    val score: Int,
    val drops: List<DropEvent> = emptyList()
)
