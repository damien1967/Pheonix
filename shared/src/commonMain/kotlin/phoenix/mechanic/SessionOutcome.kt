package phoenix.mechanic

sealed class SessionOutcome {
    object Ongoing : SessionOutcome()
    object Won : SessionOutcome()
    object Lost : SessionOutcome()
}
