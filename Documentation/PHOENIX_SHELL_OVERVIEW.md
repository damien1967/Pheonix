# Phoenix — Shell Specification
> v0.2 Draft · 2026-08-08 · Internal

---

## 1. On Naming — Why "Shell"

Before anything else, a note on the choice of word, because naming this layer correctly matters.

**"Platform"** was considered and rejected. In mobile development, "platform" means iOS or Android — the operating system you are building for. Using it for an application layer would immediately mislead any mobile developer joining the team.

**"Frontend"** was considered and rejected. In web and mobile development, "frontend" means client-side code in general — the whole app, not a layer of it.

**"Shell"** is the right word. It is honest, precise, and has solid industry precedent:

- In computing, the **shell** is the outer layer that provides access to the inner system (the kernel). The analogy is accurate: the Phoenix Shell wraps the game engine (the inner system) and provides the application around it.
- **Progressive Web App architecture** formalised "App Shell" as the minimal, reliable UI wrapper around dynamic content — exactly this concept.
- Game studios including King, Rovio, and Halfbrick use "shell" or "game shell" internally to describe the non-gameplay application wrapper.
- Apple's own documentation distinguishes "app chrome" (the wrapper UI) from content — same concept, different word.

In code and documentation: **Shell**. The full name is **Phoenix Shell**. It is not a game engine component — it is the application that contains and operates the game engine.

---

## 2. What the Shell Is

The engine specification (`PHOENIX_CORE_OVERVIEW.md`) defines what happens during a game session. It is silent on what happens before a session starts, after it ends, and everywhere in between — because those are not engine concerns.

The Shell owns all of that.

```
┌─────────────────────────────────────────────────────┐
│                   PHOENIX SHELL                     │
│                                                     │
│   Splash → Home → Game Select → [PRE-GAME]          │
│                                                     │
│                ┌─────────────────┐                  │
│                │  GAME ENGINE    │  ← one session   │
│                │  (core loop)    │                  │
│                └────────┬────────┘                  │
│                         │ session result            │
│   [POST-GAME] → Score Screen → Home → ...           │
│                                                     │
│   Settings · Help · Interstitials · Legal           │
└─────────────────────────────────────────────────────┘
```

The Shell starts the app, decides when to hand control to the engine, receives the result when a session ends, and navigates accordingly. The engine knows nothing about the Shell. The Shell knows the engine only through a single, narrow contract (see §6).

---

## 3. The Dividing Line

The cleanest way to understand what belongs where:

| Concern | Owner | Why |
|---|---|---|
| What happens when a piece is placed | Game Engine | Mechanic rule logic |
| What happens when a game session ends | Shell | App navigation decision |
| Score during a game | Game Engine | Part of game state |
| Score display after a game | Shell | Post-session presentation |
| Piece generation | Game Engine (`GenerationRule`) | Mechanic concern |
| Which game to launch next | Shell | App flow decision |
| Win/loss condition | Game Engine (`WinLossRule`) | Mechanic concern |
| What screen to show on loss | Shell | Navigation concern |
| Whether a drop condition was met | Game Engine | Mechanic trigger logic |
| Converting a drop event into a power-up token | Shell | Inventory management |
| Tracking whether a session is Pure or Augmented | Shell | App-level concern, not game logic |
| Routing scores to correct leaderboard | Shell | App-level, via GeoScoreboard flag |

**The rule:** if it requires knowledge of game rules to make the decision, it belongs in the engine. If it requires knowledge of where the user is in the app, it belongs in the Shell.

---

## 4. Shell Taxonomy

Non-game screens are grouped into five categories. Every screen in the app belongs to exactly one category.

### 4.1 Lifecycle Screens
The mandatory startup and teardown sequence. Every app launch passes through these.

| Screen | Purpose |
|---|---|
| Splash | Brand moment; app initialisation in the background. Shown once per cold launch. |
| Loading | Asset or data loading indicator. Shown only when load time is noticeable (>1s). |
| Onboarding | First-run flow only: permissions, consent, tutorial trigger. Not shown on subsequent launches. |

### 4.2 Navigation Screens
The permanent structural screens the user returns to between sessions.

| Screen | Purpose |
|---|---|
| Home / Hub | The central point of the app. Where the user lands after any session and from which all journeys begin. |
| Game Select | Shown when more than one `GameDefinition` is available. Presents the catalog. |
| Mode Select | Shown within a single `GameDefinition` when that definition offers multiple play modes. |

### 4.3 Session Boundary Screens
Screens that appear at the edges of a game session — just before it starts and immediately after it ends.

| Screen | Purpose |
|---|---|
| Pre-Game | Countdown, "ready?" confirmation, or difficulty reminder. Optional — some `GameDefinition`s skip it. |
| Score Screen | Post-session summary: score, personal best, leaderboard position, reward earned. This is the primary result screen. |
| Game Over | Terminal state reached during a session (time expired, board full, etc.) before the score summary is calculated. Optional — some mechanics go directly to Score Screen. |

### 4.4 Interstitial Screens
Screens that interrupt the main navigation flow at defined moments. All interstitials are optional and configurable per `GameDefinition`.

| Screen | Purpose |
|---|---|
| Reward Reveal | Animated reveal of an earned reward (unlock, currency, cosmetic). Shown between session end and Score Screen. |
| Achievement | Single-achievement callout, may overlay an existing screen rather than replace it. |
| Ad Placement | Third-party advertisement. Placement and frequency are Shell-level configuration, not engine concerns. Out of scope until a monetisation spec is agreed. |
| Tip / Hint | Contextual gameplay tip shown between sessions. Optional; frequency governed by Shell configuration. |

### 4.5 Support Screens
Reference and configuration screens accessible from anywhere in the Shell at any time.

| Screen | Purpose |
|---|---|
| Settings | Audio, notifications, display, account preferences. |
| How to Play | Rules explanation for the active `GameDefinition`. Content supplied by the `GameDefinition`'s theme block — the Shell provides the screen frame. |
| About / Credits | App version, team credits, acknowledgements. |
| Privacy Policy | Required by App Store and Play Store. Navigates to a hosted document or renders inline. |
| Terms of Service | Required for any account or IAP feature. Out of scope until backend spec is agreed. |

---

## 5. App State

The Shell owns **AppState** — distinct from the GameState the engine owns during a session.

AppState tracks:

| Field | Description |
|---|---|
| `currentScreen` | Which Shell screen is active, or `InGame` if the engine is running |
| `activeGameDefinition` | Which `GameDefinition` is loaded, if any |
| `lastSessionResult` | Score, outcome, augmentation status, and rewards from the most recent session |
| `playerProgress` | Cumulative data: total sessions played, badges earned, leaderboard positions |
| `settingsState` | User preferences (audio, notifications, etc.) |
| `persistentInventory` | Per-game power-up tokens sourced from Booster Pack purchases (`PURCHASED`) |
| `sessionInventory` | Per-session power-up tokens earned via in-game drops (`IN_GAME`) — discarded at session end |
| `augmentationStatus` | Whether the current session is Pure or Augmented; set the moment a `PURCHASED` token is deployed |

AppState persists across sessions. GameState does not — it is created when a session starts and discarded when it ends. The last session's result is extracted from GameState before discard and written into `lastSessionResult` in AppState. The `sessionInventory` is also discarded at session end; `persistentInventory` survives.

**Where this lives:** AppState and the screen-transition state machine that mutates it are implemented in `commonMain/kotlin/phoenix/shell/`, alongside but separate from the engine's own `engine/`, `board/`, and `piece/` packages — one shared state machine, rendered by Compose and SwiftUI separately. See `CLAUDE.md`'s file structure and `high level implementation plan.md` §3 for the full reasoning and how this compares to the engine's own state machines (`GamePiece` lifecycle, `Cell` state).

---

## 6. The Shell–Engine Contract

The Shell and the engine communicate through one narrow interface. The Shell does not reach inside the engine; the engine does not reach into the Shell.

```
Shell calls:
  engine.startSession(gameDefinition: GameDefinition) → SessionHandle

Engine emits during session:
  onStateChanged(gameState: GameState)        // Shell ignores this; presentation layer consumes it
  onDropEvent(drop: DropEvent)               // Shell converts to PowerUpToken, adds to sessionInventory
  onSessionEnded(result: SessionResult)       // Shell consumes this; navigates to Score Screen

Shell calls on session end:
  engine.teardown()
```

`SessionResult` carries everything the Shell needs: final score, win/loss flag, rewards triggered, whether a personal best was achieved, and the session's `augmentationStatus`. The Shell uses the augmentation flag when submitting to GeoScoreboard — Pure sessions route to the Pure leaderboard; Augmented sessions route to the Augmented leaderboard.

`DropEvent` carries the `PowerUpType` that has been earned. The Shell's drop handler creates a `PowerUpToken` with source `IN_GAME` and adds it to the `sessionInventory`. The engine does not know what inventory is, what source means, or that augmentation exists. It emits an event; the Shell handles the rest.

The Shell never inspects internal GameState directly.

---

## 7. What the Shell Is Not

The Shell is not a second engine. It does not contain game logic. It does not interpret scores. It does not decide what constitutes a win. All of those are `GameMechanic` concerns and live in the engine.

The Shell also has no opinion about visual style, animation, or sound. Like the engine, it emits state and structure. The presentation layer (Compose on Android, SwiftUI on iOS) renders it. Theming, transitions, and audio are a separate content pipeline that reads `GameDefinition.theme` — the same pipeline that themes the game itself.

---

## 8. Shell as Configuration

Just as a `GameDefinition` configures the engine for a specific title, a **ShellDefinition** configures the Shell. Different white-labeled titles may present different navigation flows, different interstitial policies, and different support screen content without changing Shell code.

```
ShellDefinition {
  navigation: {
    showGameSelect: false         // single-game titles go directly to Home → Pre-Game
    preGameScreen: "countdown"    // or "none"
    postGameFlow: ["rewardReveal", "scoreScreen"]
  }
  interstitials: {
    tipFrequency: "everyThirdSession"
    adsEnabled: false             // no ad placements in this title
  }
  support: {
    howToPlayContent: "ref://content/how-to-play"
    privacyPolicyUrl: "https://..."
  }
}
```

A `ShellDefinition` is authored alongside its `GameDefinition`. They are shipped together as the complete configuration for one white-labeled title.

---

## 9. Relationship to Other Specs

| Document | Relationship |
|---|---|
| `PHOENIX_CORE_OVERVIEW.md` | The engine the Shell wraps. The Shell calls into it; the engine does not call back into the Shell. Defines `DropEvent`, `PowerUp`, and the drop trigger table concept. |
| `PHOENIX_REWARDS_AND_AUGMENTATION.md` | Defines the full power-up catalogue, drop trigger tables, Booster Pack model, badge system, and Pure/Augmented rules. The Shell implements inventory management, augmentation tracking, and badge evaluation based on this spec. |
| `GeoScoreboard_System_Specification.md` | An external service the Score Screen (§4.3) integrates with. The Shell submits scores with augmentation flag; GeoScoreboard routes to the correct leaderboard. |
| `CLAUDE.md` | Architecture rules apply here too: structural entities carry no logic, configuration over code, presentation is separate. |

---

## 10. Out of Scope for This Spec

The following require their own dedicated spec before implementation:

- Ad network integration and monetisation flow
- Account management, sign-in, and backend identity
- Push notifications and re-engagement flows
- In-app purchase flow and store screens
- Multiplayer lobby and async play session management

None of these change the Shell's taxonomy or the Shell–engine contract. They add screens to categories already defined above (Support, Interstitial) or introduce new top-level categories when scoped.
