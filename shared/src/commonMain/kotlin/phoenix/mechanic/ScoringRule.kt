package phoenix.mechanic

interface ScoringRule {
    fun score(interactionResult: InteractionResult, currentScore: Int): ScoringResult
}
