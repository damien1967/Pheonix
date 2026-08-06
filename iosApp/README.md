# iosApp

`iosApp.swift`, `ContentView.swift`, and `Podfile` are pre-staged here, but the
`.xcodeproj` itself is not — that file is a binary/plist bundle only Xcode
can generate correctly.

To finish wiring this up (matches `ENVIRONMENT_SETUP_MAC.md` Stage 8–10):

1. In Xcode: **File → New → Project → iOS → App**, name it `iosApp`, save it
   into this `iosApp/` folder — let it overwrite the placeholder
   `iosApp.swift` / `ContentView.swift` with identical content, or replace
   Xcode's generated versions with these two files.
2. `cd iosApp && pod install` — links the `shared` KMP framework via the
   `Podfile` already here.
3. Open `iosApp.xcworkspace` (not `.xcodeproj`) from then on.
