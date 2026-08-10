package phoenix.mechanic

import phoenix.board.GameBoard

interface InteractionRule {
    fun resolve(board: GameBoard): InteractionResult
}
