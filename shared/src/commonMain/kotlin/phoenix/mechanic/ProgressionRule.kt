package phoenix.mechanic

interface ProgressionRule {
    fun speedMultiplierAtTurn(turnCount: Int): Double
}
