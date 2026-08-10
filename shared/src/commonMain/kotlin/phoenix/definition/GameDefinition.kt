package phoenix.definition

import phoenix.mechanic.GameMechanic
import phoenix.piece.PieceShape

data class GameDefinition(
    val board: BoardConfig,
    val pieceShapes: List<PieceShape>,
    val mechanic: GameMechanic
)
