# Phoenix — CLAUDE.md

> Engine specification: `PHOENIX_CORE_OVERVIEW.md` — read it. It is the source of truth for all architecture, terminology, and the core loop contract. This file tells you how to work; that file tells you what to build.

---

## What This Project Is

Phoenix is a **cross-platform mobile game engine** for tile and piece manipulation games. It is not a single game. Each shipped title is a `GameDefinition` — a config bundle — layered on top of the shared engine. No engine code is forked per title.

```
GAME BOARD + GAME PIECES + GAME MECHANIC = WHITE-LABELED GAME
```

The board and pieces are structurally stable across all titles. The `GameMechanic` is the only swappable layer.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Shared engine logic | Kotlin Multiplatform (KMP) — `commonMain` |
| Android presentation | Jetpack Compose |
| iOS presentation | SwiftUI |
| Build | Gradle with version catalogs (`libs.versions.toml`) |
| Testing | `kotlin.test` (common), JUnit5 (JVM), XCTest (iOS) |
| CI | GitHub Actions |
| Code quality | Ktlint (formatting), Detekt (static analysis) |

All engine logic — core loop, data models, rule-sets, `GameDefinition` — lives in `commonMain`. Presentation is platform-specific and strictly separated from engine logic. Do not bleed engine logic into the presentation layer, and do not bleed platform code into `commonMain`.

---

## Developer Profile — Read This First

The developer has prior experience in Swift/SwiftUI, Objective-C, C/C++, Pascal, and some Kotlin. They are returning to active development after a gap. Code must be **immediately readable without needing to look anything up**.

**Hard rules:**
- Never suggest React Native. It is not on the table.
- Never suggest a clever algorithm when a simple one exists. Simple and correct beats fast and opaque, always.
- Never use language features that require deep Kotlin internals knowledge — no heavy DSL builders, no reified generics tricks, no complex coroutine graphs. Straightforward sequential logic is preferred.
- Do not use reflection, annotation processors beyond standard KMP tooling, or runtime code generation.
- When explaining code, use SwiftUI mental models as a reference point where helpful — the developer will recognise them.

**Style defaults:**
- Explicit over implicit. Write out the full type, the full name, the full condition.
- Named parameters everywhere. Data classes over tuples or anonymous structures.
- `val` over `var`. Immutability by default; mutations are explicit and traceable.
- Short functions. One function, one job. If a function needs a comment to explain what it does, it should be split or renamed instead.
- Prefer `sealed class` and explicit dispatch over string-keyed registries or `when` on raw strings.

---

## Architecture Rules

These are non-negotiable. Raise a concern rather than silently violate one.

**1. The core loop has six steps and mechanic hooks at three of them.**
The mechanic module is invoked only at step 2 (Placement Validator), step 4 (Interaction Resolver), and step 6 (State Broadcast & Replenish). The engine never calls mechanic logic anywhere else. See `PHOENIX_CORE_OVERVIEW.md §4`.

**2. Structural entities carry no rule logic.**
`GameBoard`, `Cell`, `GamePiece`, and `PieceSource` are pure data and state containers. They do not know what a valid placement means, what a resolved interaction means, or what piece comes next. Only `GameMechanic` knows those things.

**3. A `GameDefinition` is config, not code.**
Creating a new game means authoring a new `GameDefinition` — selecting and parameterising the seven rule-sets. No engine code changes. If engine code needs to change to ship a new game, that is a design failure.

**4. `PieceSource` is deliberately dumb.**
It is a slot container. Generation policy, tray size, and refill behaviour are all mechanic concerns defined in the `GenerationRule`. Do not add any fill or selection logic to `PieceSource`.

**5. Modifiers are a mechanic concern.**
Power-ups and temporary rule adjustments live in the mechanic module. They are not a board entity or a piece property.

**6. Presentation is outside the engine contract.**
The engine emits state. The presentation layer consumes it. Theming, VFX, animation, and audio are a separate pipeline. Do not design engine interfaces around presentation needs.

---

## Testing Philosophy

Testing is not optional and is not deferred. It is part of building the feature.

**Workflow for every piece of new functionality:**
1. Write the test first — specify the behaviour before writing the implementation
2. Write the minimum implementation that passes the test
3. Refactor if needed; tests stay green throughout

**Coverage expectations:**
- Engine core loop: every step, every branch — 100%
- Every rule-set implementation: 100% with representative inputs including edge cases
- `GameDefinition` loading and validation: 100%
- Platform UI: snapshot and integration tests where practical

**Test naming:** `given_<context>_when_<action>_then_<outcome>`. A test name is a specification statement. It should be readable as plain English.

**Release gate:** Nothing ships unless the full test suite passes in CI. There are no exceptions and no manual overrides.

**When asked to add a feature:** propose the tests alongside the implementation plan, not after.

---

## Canonical Terminology

Use these terms verbatim in all code, comments, and discussion. Do not invent synonyms.

| Use | Never use |
|---|---|
| `GameBoard` | board, grid, field, arena, playfield |
| `Cell` | tile, square, slot *(when referring to board locations)* |
| `GamePiece` | piece, block, token, shape *(use `PieceShape` for the template)* |
| `PieceShape` | template, pattern, layout, mask |
| `PieceSource` | tray, queue, deck *(these are valid source *types*, not synonyms for the container)* |
| `GameMechanic` | ruleset, logic, behaviour, rules |
| `GameDefinition` | config, profile, variant, skin |
| `Modifier` | power-up *(use "power-up" only in player-facing copy)*, buff, boost |
| `InteractionRule` | clear logic, merge logic, resolution logic |
| `GenerationRule` | spawn logic, piece supply, bag logic |

---

## File Structure

```
/
├── CLAUDE.md                            ← this file
├── PHOENIX_CORE_OVERVIEW.md             ← engine spec, source of truth
├── ENVIRONMENT_SETUP_MAC.md             ← dev environment setup guide
├── composeApp/                          ← Android / Jetpack Compose presentation
├── iosApp/                              ← iOS / SwiftUI presentation
└── shared/
    └── src/
        ├── commonMain/kotlin/phoenix/
        │   ├── engine/                  ← core loop, board mutation, state broadcast
        │   ├── board/                   ← GameBoard, Cell
        │   ├── piece/                   ← GamePiece, PieceShape, PieceSource
        │   ├── mechanic/                ← GameMechanic interface, all rule-set interfaces
        │   ├── definition/              ← GameDefinition, loading, validation
        │   ├── modifier/                ← Modifier model, active modifier stack
        │   └── shell/                   ← AppState, ShellDefinition, screen-transition state machine
        └── commonTest/kotlin/phoenix/   ← mirrors commonMain exactly
```

Every new module in `commonMain` gets a corresponding test module in `commonTest`. No exceptions.

---

## What Is Out of Scope

Do not implement these without a dedicated spec. If a task touches one of these areas, say so and stop.

- Presentation theming, skins, VFX, or audio
- Backend systems: accounts, leaderboards, live-ops, IAP
- Multiplayer or async play
- Analytics or telemetry

---

## Working With Me

- If you are about to do something that violates an architecture rule, say so before doing it and propose an alternative.
- If a requirement is ambiguous, ask one specific question rather than making an assumption and building on it.
- If you produce a code change, also produce or update the tests for it in the same response.
- When suggesting approaches, give me the simple one first. If there is a more sophisticated option worth knowing about, mention it briefly after — do not lead with it.
- Do not pad responses. Short and correct is better than long and hedged.
