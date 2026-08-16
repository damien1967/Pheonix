package phoenix.definition

import phoenix.board.LevelSource

object GameDefinitionValidator {

    fun validate(definition: GameDefinition): ValidationResult {
        val levels = definition.levels
        if (levels is LevelSource.Authored && levels.sequence.levels.isEmpty()) {
            return ValidationResult.Invalid("LevelSequence must contain at least one LevelConfig")
        }
        if (definition.pieceShapes.isEmpty()) {
            return ValidationResult.Invalid("At least one PieceShape is required")
        }
        return ValidationResult.Valid
    }
}
