package phoenix.mechanic

interface GameMechanic {
    val placement: PlacementRule
    val interaction: InteractionRule
    val progression: ProgressionRule
    val scoring: ScoringRule
    val reward: RewardRule
    val winLoss: WinLossRule
    val generation: GenerationRule
}
