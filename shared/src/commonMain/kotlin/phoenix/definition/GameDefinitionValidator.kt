package phoenix.definition

object GameDefinitionValidator {

    fun validate(definition: GameDefinition): ValidationResult {
        if (definition.board.rowCount <= 0 || definition.board.columnCount <= 0) {
            return ValidationResult.Invalid("Board dimensions must be positive")
        }
        if (definition.pieceShapes.isEmpty()) {
            return ValidationResult.Invalid("At least one PieceShape is required")
        }
        return ValidationResult.Valid
    }
}
