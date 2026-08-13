package phoenix.mechanic

sealed class LevelOutcome {
    object LevelCleared : LevelOutcome()
    object GameOver : LevelOutcome()
    object FinalLevelCleared : LevelOutcome()
    object EndlessEnded : LevelOutcome()
}
