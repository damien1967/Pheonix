package phoenix.board

import kotlin.test.Test
import kotlin.test.assertEquals

class LevelSourceTest {

    private val levelZero = LevelConfig(shape = GridShape.Rectangular(width = 4, height = 4))
    private val levelOne = LevelConfig(shape = GridShape.Rectangular(width = 5, height = 5))

    @Test
    fun given_authoredSource_when_levelAtRequested_then_returnsSequenceEntryAtIndex() {
        val source = LevelSource.Authored(LevelSequence(levels = listOf(levelZero, levelOne)))

        assertEquals(levelOne, source.levelAt(1))
    }

    @Test
    fun given_generatedSource_when_levelAtRequested_then_returnsGeneratorOutputForIndex() {
        val source = LevelSource.Generated { index ->
            LevelConfig(shape = GridShape.Rectangular(width = index, height = index))
        }

        assertEquals(GridShape.Rectangular(width = 7, height = 7), source.levelAt(7).shape)
    }

    @Test
    fun given_compositeSourceWithOverrideAtIndex_when_levelAtRequested_then_returnsAuthoredOverride() {
        val bonusLevel = LevelConfig(shape = GridShape.Rectangular(width = 9, height = 9))
        val source = LevelSource.Composite(
            generator = LevelGenerator { index -> LevelConfig(shape = GridShape.Rectangular(width = index, height = index)) },
            authoredOverrides = mapOf(10 to bonusLevel)
        )

        assertEquals(bonusLevel, source.levelAt(10))
    }

    @Test
    fun given_compositeSourceWithoutOverrideAtIndex_when_levelAtRequested_then_fallsBackToGenerator() {
        val source = LevelSource.Composite(
            generator = LevelGenerator { index -> LevelConfig(shape = GridShape.Rectangular(width = index, height = index)) },
            authoredOverrides = mapOf(10 to LevelConfig(shape = GridShape.Rectangular(width = 99, height = 99)))
        )

        assertEquals(GridShape.Rectangular(width = 3, height = 3), source.levelAt(3).shape)
    }
}
