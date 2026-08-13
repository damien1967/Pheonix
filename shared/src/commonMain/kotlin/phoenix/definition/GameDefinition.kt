package phoenix.definition

import phoenix.board.LevelSequence
import phoenix.mechanic.GameMechanic
import phoenix.piece.PieceShape

data class GameDefinition(
    val levels: LevelSequence,
    val pieceShapes: List<PieceShape>,
    val mechanic: GameMechanic
)
