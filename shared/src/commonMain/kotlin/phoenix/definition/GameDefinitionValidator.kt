package phoenix.definition

object GameDefinitionValidator {

    fun validate(definition: GameDefinition): ValidationResult {
        if (definition.levels.levels.isEmpty()) {
            return ValidationResult.Invalid("LevelSequence must contain at least one LevelConfig")
        }
        if (definition.pieceShapes.isEmpty()) {
            return ValidationResult.Invalid("At least one PieceShape is required")
        }
        return ValidationResult.Valid
    }
}
