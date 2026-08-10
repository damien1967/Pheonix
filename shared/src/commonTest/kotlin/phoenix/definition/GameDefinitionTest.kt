package phoenix.definition

import phoenix.board.BoardPosition
import phoenix.board.GameBoard
import phoenix.mechanic.GameMechanic
import phoenix.mechanic.GenerationRule
import phoenix.mechanic.InteractionResult
import phoenix.mechanic.InteractionRule
import phoenix.mechanic.PlacementRule
import phoenix.mechanic.ProgressionRule
import phoenix.mechanic.Reward
import phoenix.mechanic.RewardRule
import phoenix.mechanic.ScoringRule
import phoenix.mechanic.SessionOutcome
import phoenix.mechanic.WinLossRule
import phoenix.piece.CellOffset
import phoenix.piece.GamePiece
import phoenix.piece.PieceLifecycleState
import phoenix.piece.PieceShape
import phoenix.piece.PieceSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameDefinitionTest {

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

    private fun definition(
        rowCount: Int = 8,
        columnCount: Int = 8,
        pieceShapes: List<PieceShape> = listOf(samplePiece.shape)
    ) = GameDefinition(
        board = BoardConfig(rowCount = rowCount, columnCount = columnCount),
        pieceShapes = pieceShapes,
        mechanic = noOpMechanic
    )

    @Test
    fun given_validGameDefinition_when_validated_then_resultIsValid() {
        assertEquals(ValidationResult.Valid, GameDefinitionValidator.validate(definition()))
    }

    @Test
    fun given_boardWithZeroRows_when_validated_then_resultIsInvalid() {
        val result = GameDefinitionValidator.validate(definition(rowCount = 0))
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun given_emptyPieceShapeList_when_validated_then_resultIsInvalid() {
        val result = GameDefinitionValidator.validate(definition(pieceShapes = emptyList()))
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun given_gameDefinition_when_constructed_then_mechanicRuleSetsAccessible() {
        val gameDefinition = definition()
        assertEquals(SessionOutcome.Ongoing, gameDefinition.mechanic.winLoss.outcome(
            board = GameBoard.create(rowCount = 1, columnCount = 1),
            pieceSource = PieceSource.create(slotCount = 1),
            score = 0
        ))
    }
}
