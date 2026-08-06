package phoenix

// Temporary: Kotlin/Native emits no framework at all when commonMain has zero
// declarations, which breaks the Xcode/CocoaPods build. Delete this once real
// engine code exists in shared/.
object BuildProbe {
    val isReady: Boolean = true
}
