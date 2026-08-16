package phoenix.definition

import phoenix.board.LevelSource
import phoenix.mechanic.DropTrigger
import phoenix.mechanic.GameMechanic
import phoenix.mechanic.LevelMode
import phoenix.piece.PieceShape

data class GameDefinition(
    val levels: LevelSource,
    val pieceShapes: List<PieceShape>,
    val mechanic: GameMechanic,
    val levelMode: LevelMode,
    val drops: List<DropTrigger> = emptyList()
)
