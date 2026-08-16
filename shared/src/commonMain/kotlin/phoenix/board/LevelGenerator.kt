package phoenix.board

fun interface LevelGenerator {
    fun levelAt(index: Int): LevelConfig
}
