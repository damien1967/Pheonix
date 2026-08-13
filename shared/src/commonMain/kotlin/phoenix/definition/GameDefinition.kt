package phoenix.definition

import phoenix.mechanic.GameMechanic
import phoenix.piece.PieceShape

data class GameDefinition(
    val levels: LevelSequence,
    val pieceShapes: List<PieceShape>,
    val mechanic: GameMechanic
)
