package phoenix.mechanic

import phoenix.board.BoardPosition
import phoenix.board.GameBoard
import phoenix.board.GridShape
import phoenix.piece.CellOffset
import phoenix.piece.GamePiece
import phoenix.piece.PieceLifecycleState
import phoenix.piece.PieceShape
import phoenix.piece.PieceSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameMechanicTest {

    private val samplePiece = GamePiece(
        shape = PieceShape(offsets = listOf(CellOffset(row = 0, column = 0))),
        lifecycleState = PieceLifecycleState.InSource
    )

    private val noOpMechanic = object : GameMechanic {
        override val placement = object : PlacementRule {
            override fun isLegalPlacement(board: GameBoard, piece: GamePiece, origin: BoardPosition) = true
        }
        override val interaction = object : InteractionRule {
            override fun resolve(board: GameBoard) = InteractionResult(board = board, resolvedCellCount = 0)
        }
        override val progression = object : ProgressionRule {
            override fun speedMultiplierAtTurn(turnCount: Int) = 1.0
        }
        override val scoring = object : ScoringRule {
            override fun score(interactionResult: InteractionResult, currentScore: Int) = currentScore
        }
        override val reward = object : RewardRule {
            override fun rewardsEarnedAt(score: Int): List<Reward> = emptyList()
        }
        override val winLoss = object : WinLossRule {
            override fun outcome(board: GameBoard, pieceSource: PieceSource, score: Int) = SessionOutcome.Ongoing
        }
        override val generation = object : GenerationRule {
            override fun nextPiece(pieceSource: PieceSource) = samplePiece
        }
    }

    @Test
    fun given_gameMechanic_when_constructed_then_allSevenRuleSetsAreAccessible() {
        val board = GameBoard.create(shape = GridShape.Rectangular(width = 1, height = 1))
        val source = PieceSource.create(slotCount = 1)

        assertTrue(noOpMechanic.placement.isLegalPlacement(board, samplePiece, BoardPosition(0, 0)))
        assertEquals(0, noOpMechanic.interaction.resolve(board).resolvedCellCount)
        assertEquals(1.0, noOpMechanic.progression.speedMultiplierAtTurn(turnCount = 5))
        assertEquals(10, noOpMechanic.scoring.score(InteractionResult(board, 0), currentScore = 10))
        assertEquals(emptyList(), noOpMechanic.reward.rewardsEarnedAt(score = 100))
        assertEquals(SessionOutcome.Ongoing, noOpMechanic.winLoss.outcome(board, source, score = 0))
        assertEquals(samplePiece, noOpMechanic.generation.nextPiece(source))
    }
}
