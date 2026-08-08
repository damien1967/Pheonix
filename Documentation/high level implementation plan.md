# Phoenix — High-Level Implementation Plan
> v0.1 Draft · 2026-08-08 · Internal

Companion to `PHOENIX_CORE_OVERVIEW.md` (the engine spec) and `PHOENIX_SHELL_OVERVIEW.md` (the shell spec). Those documents say what to build. This document says what order to build it in, and why that order is the fastest path to proving both specs actually hold up.

---

## 1. Where We Are Today

Honest inventory of the scaffold, checked directly against the repo:

- `shared/src/commonMain/kotlin/phoenix/{board,piece,mechanic,definition,modifier,engine}/` — folders exist, all empty except `.gitkeep`
- `shared/src/commonMain/kotlin/phoenix/BuildProbe.kt` — a placeholder proving the KMP build compiles for Android and iOS
- No `phoenix/shell/` package exists yet anywhere — the Shell layer has no code, only a spec
- CI (`jvm-and-android` + `ios` jobs), `WAY_OF_WORKING.md`, and the GitHub Issues/Projects planning setup are in place and working
- No `GameDefinition` has been authored — not even the reference Grid-Fill Clear example from `PHOENIX_CORE_OVERVIEW.md` §10

In short: the walls of the house are marked out, nothing is built yet. That's a good, honest starting point — it means every decision below is still open.

---

## 2. Guiding Strategy

Don't build engine breadth or Shell breadth first. Build the thinnest possible **vertical slice** that exercises the full core loop, the full Shell–engine contract, and one visible screen — using the **Grid-Fill Clear reference mechanic** already worked out in `PHOENIX_CORE_OVERVIEW.md` §10 as the test subject.

Reasons:
- Grid-Fill Clear is explicitly designed in the spec to prove "the engine reproduces a known genre through configuration alone." It's the cheapest possible way to test the central claim of the whole project — `GameDefinition` as config, not code.
- A vertical slice surfaces contract problems (Shell↔engine, `GameDefinition`↔engine) immediately, while they're still cheap to fix. Building all seven rule-sets in the abstract, or the full Shell screen taxonomy, before anything runs end-to-end risks discovering a contract mismatch after a lot of code already depends on it.
- It matches the testing philosophy in `CLAUDE.md`: every phase below produces a failing test first, per step/rule-set, before implementation.

Everything not needed for that one slice — a second `GameDefinition`, `Modifier`s, ads, accounts, the rest of the Shell taxonomy — is deliberately deferred (§7).

---

## 3. State Machines — What They Are and Where They Live

**Decided.** The architecture has three state machines, not one, and they don't all live in the same place:

| State machine | States | Lives in | Who drives transitions |
|---|---|---|---|
| `GamePiece` lifecycle | `in-source → held → preview(valid/invalid) → placed → resolved/discarded` (`PHOENIX_CORE_OVERVIEW.md` §"Game Piece") | `piece/` (commonMain) | Engine's Placement Validator / Interaction Resolver |
| `Cell` state | `empty, occupied, blocked, marked` (`PHOENIX_CORE_OVERVIEW.md` §"Game Board") | `board/` (commonMain) | Engine's Board Mutation step |
| Shell screen/navigation | `Splash → Home → GameSelect → PreGame → InGame → ScoreScreen → Home …` (`PHOENIX_SHELL_OVERVIEW.md` §4) | `shell/` (commonMain) | The Shell itself, on `onSessionEnded` and user navigation actions |

`AppState` and screen-transition logic live in `commonMain/kotlin/phoenix/shell/`, the same as the engine — one shared state machine, rendered by Compose and SwiftUI separately. This mirrors the exact reasoning already applied to the engine (`CLAUDE.md` rule 6: presentation is outside the engine contract) and avoids writing the same navigation logic twice. Only screen *rendering* is platform-specific.

Keep the machines themselves boring — a sealed class per set of states, one pure `reduce(current, event) -> next` function per transition, no state-machine framework, no coroutine graph. This matches `CLAUDE.md`'s style default ("prefer sealed class and explicit dispatch") and is unit-testable with the same `given_<state>_when_<event>_then_<newState>` naming as everything else.

One distinction worth keeping straight: `GamePiece` and `Cell` state are *data the structural entity carries* — the entity holds its current state but never decides whether a transition is valid (that's the mechanic's job, per `CLAUDE.md` rule 2). The Shell's screen state machine is different — the Shell *is* the authority on its own transitions; there's no separate "shell mechanic" the way there's a `GameMechanic` for game rules.

`CLAUDE.md`'s file structure table has been updated with a `shell/` entry to reflect this.

---

## 4. Phased Plan

| Phase | Goal | Key deliverables | Representative tests | Exit criteria |
|---|---|---|---|---|
| **1. Structural entities** | Pure data/state containers, no rule logic (`CLAUDE.md` rule 2) | `GameBoard`, `Cell`, `GamePiece`, `PieceShape`, `PieceSource` in `board/` and `piece/` | `given_emptyBoard_when_cellQueried_then_stateIsEmpty`; `given_heldPiece_when_rotated_then_shapeOffsetsRotate`; `given_pieceSource_when_slotFilled_then_slotCountIncreases` | All structural entities exist, fully tested, and contain zero placement/interaction/generation logic |
| **2. Mechanic contract + `GameDefinition`** | Define the seven rule-set interfaces and the config shape that selects them | `GameMechanic` interface; `PlacementRule`, `InteractionRule`, `ProgressionRule`, `ScoringRule`, `RewardRule`, `WinLossRule`, `GenerationRule` interfaces in `mechanic/`; `GameDefinition` data class + loader in `definition/` | `given_validGameDefinition_when_loaded_then_allSevenRuleSetsResolve`; `given_missingRuleSet_when_loaded_then_validationFails` | A `GameDefinition` can be constructed and validated end-to-end with no concrete rule-set behaviour yet |
| **3. Reference mechanic: Grid-Fill Clear** | Concrete implementation of §10's worked example — the first real proof that config alone produces a game | One implementation per rule-set (no-overlap placement, full-line clear, turn-based progression, cells+lines scoring, threshold reward, board-full loss, random-bag generation) | `given_fullRow_when_placementResolves_then_rowClears`; `given_simultaneousRowAndColumn_when_cleared_then_bothResolveTogether`; `given_noLegalPlacement_when_checked_then_lossTriggers` | Grid-Fill Clear's rule-sets pass their own tests in isolation, before being wired into the loop |
| **4. Engine core loop** | Wire the six-step loop (`CLAUDE.md` rule 1), hooks at steps 2/4/6 only | `engine/` loop implementation calling into Grid-Fill Clear's `GameMechanic` | `given_invalidPlacement_when_validated_then_pieceReturnsToSource`; `given_validPlacement_when_mutated_then_boardCommits`; `given_openTraySlot_when_replenished_then_generationRuleSupplied` | 100% branch coverage of the six steps, using Grid-Fill Clear as the only mechanic under test — this is the first point where "does the architecture actually work" gets answered |
| **5. Shell–engine contract** | Implement §6 of the Shell spec: `startSession`, `onStateChanged`, `onSessionEnded`, `teardown` | `shell/` session-handle wiring (see §3) | `given_sessionEnds_when_resultEmitted_then_onSessionEndedCarriesFinalScore`; `given_teardownCalled_then_engineStateDiscarded` | The Shell can start a Grid-Fill Clear session and receive a `SessionResult` without inspecting `GameState` directly |
| **6. Minimal `AppState` + walking-skeleton screens** | Only the screens needed to close the loop: Home → Pre-Game (or skip) → [session] → Score Screen → Home | `AppState` fields (`currentScreen`, `activeGameDefinition`, `lastSessionResult`); a bare-bones `ShellDefinition` for Grid-Fill Clear | `given_sessionResult_when_received_then_lastSessionResultUpdates`; `given_scoreScreenDismissed_then_currentScreenReturnsToHome` | A full lifecycle transition compiles and is tested, with no rendering yet |
| **7. One platform presentation** | Render the walking skeleton on one platform first, not two at once | Recommend **SwiftUI first** — it's the developer's strongest existing skill (per `CLAUDE.md`'s developer profile), so the *new* thing being tested is the Kotlin engine/shell, not also a new UI framework | Manual verification via the `run` workflow, plus any Compose/SwiftUI snapshot tests practical at this stage | Grid-Fill Clear is tappable and playable on a real device or simulator |
| **8. Vertical slice integration** | The milestone: Home → tap Play → engine session → place pieces → lines clear → board fills → Score Screen → Home, on one platform, end to end | Wiring only — no new engine/Shell code if 1–7 were done right | Manual playthrough + full existing automated suite green in CI | This is the proof-of-concept moment for both specs simultaneously. Don't start a second `GameDefinition` or the second platform before this milestone is reached. |

---

## 5. What's Deliberately Deferred

Not because these are unimportant — because building them before Phase 8 risks designing against an unproven contract.

| Deferred item | Why it waits |
|---|---|
| Second `GameDefinition` | Proves config-reuse, but only meaningful once one `GameDefinition` has been proven end-to-end |
| `Modifier`s | Explicitly optional per `GameDefinition` (`PHOENIX_CORE_OVERVIEW.md` §8) — the simplest config has none |
| Second platform (Jetpack Compose port) | Port the *proven* SwiftUI slice, don't build both simultaneously and debug two unknowns at once |
| Ktlint / Detekt wiring | Named in `CLAUDE.md` tech stack but not yet in the Gradle build — housekeeping, not a blocker to proving the architecture |
| `GeoScoreboard` integration | Explicitly an external service the Score Screen integrates with (`PHOENIX_SHELL_OVERVIEW.md` §9) — out of scope until the Score Screen itself exists |
| Interstitials, ads, onboarding, settings, accounts, IAP | All explicitly out of scope in both specs until their own dedicated specs are written |

---

## 6. Suggested Next Step

Phases 1–4 map cleanly onto individual GitHub Issues (one rule-set or one structural entity per ticket, per the labels already set up: `engine`, `board`, `piece`, `mechanic`, `definition`). Once the `gh` CLI has `project` scope confirmed working, this plan can be broken into tickets on the Phoenix Roadmap project directly — say the word and I'll do that instead of you copying phases into issues by hand.
