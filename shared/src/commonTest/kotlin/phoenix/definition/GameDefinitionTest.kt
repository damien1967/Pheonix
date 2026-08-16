package phoenix.definition

import phoenix.board.BoardPosition
import phoenix.board.GameBoard
import phoenix.board.GridShape
import phoenix.board.LevelConfig
import phoenix.board.LevelSequence
import phoenix.board.LevelSource
import phoenix.mechanic.DropCondition
import phoenix.mechanic.DropOutcome
import phoenix.mechanic.DropTrigger
import phoenix.mechanic.GameMechanic
import phoenix.mechanic.GenerationRule
import phoenix.mechanic.InteractionResult
import phoenix.mechanic.InteractionRule
import phoenix.mechanic.LevelMode
import phoenix.mechanic.LevelOutcome
import phoenix.mechanic.PlacementRule
import phoenix.mechanic.WeightedDrop
import phoenix.mechanic.ProgressionRule
import phoenix.mechanic.RewardResult
import phoenix.mechanic.RewardRule
import phoenix.mechanic.ScoringResult
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
            override fun nextLevel(levelSource: LevelSource, completedLevelIndex: Int): LevelConfig {
                check(levelSource is LevelSource.Authored)
                return levelSource.sequence.levels[completedLevelIndex + 1]
            }
            override fun levelOutcome(
                sessionOutcome: SessionOutcome,
                levelSource: LevelSource,
                completedLevelIndex: Int
            ) = LevelOutcome.FinalLevelCleared
        }
        override val scoring = object : ScoringRule {
            override fun score(interactionResult: InteractionResult, currentScore: Int) =
                ScoringResult(score = currentScore)
        }
        override val reward = object : RewardRule {
            override fun rewardsEarnedAt(score: Int) = RewardResult(rewards = emptyList())
        }
        override val winLoss = object : WinLossRule {
            override fun outcome(board: GameBoard, pieceSource: PieceSource, score: Int) = SessionOutcome.Ongoing
        }
        override val generation = object : GenerationRule {
            override fun nextPiece(pieceSource: PieceSource) = samplePiece
        }
    }

    private fun definition(
        levels: LevelSource = LevelSource.Authored(
            LevelSequence(levels = listOf(LevelConfig(shape = GridShape.Rectangular(width = 8, height = 8))))
        ),
        pieceShapes: List<PieceShape> = listOf(samplePiece.shape),
        levelMode: LevelMode = LevelMode.ENDLESS,
        drops: List<DropTrigger> = emptyList()
    ) = GameDefinition(
        levels = levels,
        pieceShapes = pieceShapes,
        mechanic = noOpMechanic,
        levelMode = levelMode,
        drops = drops
    )

    @Test
    fun given_validGameDefinition_when_validated_then_resultIsValid() {
        assertEquals(ValidationResult.Valid, GameDefinitionValidator.validate(definition()))
    }

    @Test
    fun given_emptyLevelSequence_when_validated_then_resultIsInvalid() {
        val result = GameDefinitionValidator.validate(
            definition(levels = LevelSource.Authored(LevelSequence(levels = emptyList())))
        )
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun given_generatedLevelSource_when_validated_then_resultIsValid() {
        val generatedSource = LevelSource.Generated { index ->
            LevelConfig(shape = GridShape.Rectangular(width = 8 + index, height = 8 + index))
        }
        val result = GameDefinitionValidator.validate(definition(levels = generatedSource))
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun given_emptyPieceShapeList_when_validated_then_resultIsInvalid() {
        val result = GameDefinitionValidator.validate(definition(pieceShapes = emptyList()))
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun given_stagedLevelMode_when_constructed_then_levelModeAccessible() {
        val gameDefinition = definition(levelMode = LevelMode.STAGED)
        assertEquals(LevelMode.STAGED, gameDefinition.levelMode)
    }

    @Test
    fun given_noDropsSupplied_when_constructed_then_dropsDefaultsToEmpty() {
        assertEquals(emptyList(), definition().drops)
    }

    @Test
    fun given_deterministicAndWeightedDropTriggers_when_constructed_then_dropsAccessible() {
        val drops = listOf(
            DropTrigger(
                condition = DropCondition.Engine.AnyLineCleared,
                outcome = DropOutcome.Deterministic(powerUpIds = listOf("PieceSwap"))
            ),
            DropTrigger(
                condition = DropCondition.Engine.SimultaneousClears(lineCount = 4),
                outcome = DropOutcome.Weighted(
                    odds = listOf(
                        WeightedDrop(powerUpId = "LineBomb", weightPercent = 5),
                        WeightedDrop(powerUpId = "ScoreMultiplier", weightPercent = 10)
                    )
                )
            )
        )

        val gameDefinition = definition(drops = drops)

        assertEquals(drops, gameDefinition.drops)
    }

    @Test
    fun given_shellEvaluatedDropTrigger_when_constructed_then_dropsAccessible() {
        val drops = listOf(
            DropTrigger(
                condition = DropCondition.Shell.PersonalBestInSession,
                outcome = DropOutcome.Deterministic(powerUpIds = listOf("Undo"))
            )
        )

        val gameDefinition = definition(drops = drops)

        assertEquals(drops, gameDefinition.drops)
    }

    @Test
    fun given_gameDefinition_when_constructed_then_mechanicRuleSetsAccessible() {
        val gameDefinition = definition()
        assertEquals(SessionOutcome.Ongoing, gameDefinition.mechanic.winLoss.outcome(
            board = GameBoard.create(shape = GridShape.Rectangular(width = 1, height = 1)),
            pieceSource = PieceSource.create(slotCount = 1),
            score = 0
        ))
    }
}
