package phoenix.definition

import phoenix.board.LevelSource
import phoenix.mechanic.DropCondition
import phoenix.mechanic.DropOutcome
import phoenix.mechanic.DropTrigger
import phoenix.mechanic.LevelMode

object GameDefinitionValidator {

    fun validate(definition: GameDefinition): ValidationResult {
        val levels = definition.levels

        if (levels is LevelSource.Authored && levels.sequence.levels.isEmpty()) {
            return ValidationResult.Invalid("LevelSequence must contain at least one LevelConfig")
        }
        if (definition.levelMode == LevelMode.STAGED &&
            levels is LevelSource.Authored &&
            levels.sequence.levels.size < 2
        ) {
            return ValidationResult.Invalid("Staged level mode requires at least two LevelConfigs")
        }
        if (levels is LevelSource.Composite && levels.authoredOverrides.keys.any { it < 0 }) {
            return ValidationResult.Invalid("LevelSource.Composite authored override indices must not be negative")
        }
        if (definition.pieceShapes.isEmpty()) {
            return ValidationResult.Invalid("At least one PieceShape is required")
        }

        dropsFailureReason(definition.drops)?.let { return ValidationResult.Invalid(it) }

        return ValidationResult.Valid
    }

    private fun dropsFailureReason(drops: List<DropTrigger>): String? {
        for (trigger in drops) {
            conditionFailureReason(trigger.condition)?.let { return it }
            outcomeFailureReason(trigger.outcome)?.let { return it }
        }
        return null
    }

    private fun conditionFailureReason(condition: DropCondition): String? {
        return when (condition) {
            DropCondition.Engine.AnyLineCleared -> null
            DropCondition.Shell.PersonalBestInSession -> null
            is DropCondition.Engine.SimultaneousClears ->
                if (condition.lineCount < 2) "SimultaneousClears requires a lineCount of at least 2" else null
            is DropCondition.Engine.ScoreMilestones -> {
                val thresholds = condition.thresholds
                when {
                    thresholds.isEmpty() -> "ScoreMilestones requires at least one threshold"
                    thresholds.any { it <= 0 } -> "ScoreMilestones thresholds must be positive"
                    thresholds.zipWithNext().any { (earlier, later) -> earlier >= later } ->
                        "ScoreMilestones thresholds must be strictly increasing"
                    else -> null
                }
            }
        }
    }

    private fun outcomeFailureReason(outcome: DropOutcome): String? {
        return when (outcome) {
            is DropOutcome.Deterministic ->
                if (outcome.powerUpIds.isEmpty() || outcome.powerUpIds.any { it.isBlank() }) {
                    "Deterministic drop outcome requires at least one non-blank powerUpId"
                } else {
                    null
                }
            is DropOutcome.Weighted -> {
                val odds = outcome.odds
                when {
                    odds.isEmpty() -> "Weighted drop outcome requires at least one WeightedDrop"
                    odds.any { it.weightPercent <= 0 || it.powerUpId.isBlank() } ->
                        "WeightedDrop entries require a positive weightPercent and a non-blank powerUpId"
                    outcome.totalWeightPercent > 100 -> "Weighted drop odds must not sum to more than 100"
                    else -> null
                }
            }
        }
    }
}
