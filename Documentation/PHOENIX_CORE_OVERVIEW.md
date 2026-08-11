# Phoenix Core — Engine Implementation Overview
> v0.3 Draft · 2026-08-11 · Internal

## What Phoenix Is

Phoenix is a **multi-game engine**, not a single game. Each shipped title is a *configuration* of the engine — no code fork required. Every game produced by the engine reduces to the same identity:

```
GAME BOARD + GAME PIECES + GAME MECHANIC = WHITE-LABELED GAME
```

The board and pieces are structurally stable across the entire catalog. The **mechanic** is the only swappable layer — it decides what placing a piece means and is the only thing that changes between shippable games.

---

## Core Taxonomy

| Term | Definition |
|---|---|
| `GameBoard` | The play surface for one session. Owns the set of cells defined by the current `LevelConfig`. Agnostic to shape, dimensions, and zone logic. |
| `Cell` | One addressable board location within the `GridShape`. Holds occupancy plus optional tags (color, type, blocked). |
| `GridShape` | Defines which cells exist on the board. Either `Rectangular(width, height)` — the default — or `CellSet(coordinates)` for irregular shapes (hex outline, L-shape, etc.). Cells outside the `GridShape` are absent; they are not blocked, they simply do not exist. |
| `Zone` | A named sub-region of cells within the board. Defined in `LevelConfig`; used exclusively by the `InteractionRule` for zone-based clear conditions (e.g. a Woodoku-style 3×3 square that clears when full). The board stores zones as metadata but does not interpret them. |
| `LevelConfig` | The complete board configuration for one session: a `GridShape`, a set of `blockedCells`, and an optional list of `Zone`s. Supplied by the `ProgressionRule` at session start. |
| `LevelSequence` | An authored, ordered list of `LevelConfig`s within a `GameDefinition`, used by staged games. The `ProgressionRule` advances through the sequence as the player completes each level. Endless games use a single `LevelConfig` or a `LevelGenerator` instead. |
| `GamePiece` | A placeable object made of one or more cells in a shape. Carries its own state: position, rotation, tags, lifecycle. |
| `PieceShape` | The cell-offset template a piece is stamped from. |
| `PieceSource` | The runtime slot container offering pieces to the player — tray, queue, or deck. Structural only; fill logic belongs to the mechanic's Generation Rule. |
| `GameMechanic` | The ruleset governing how placed pieces interact, score, and progress. The differentiating layer between white-labeled games. |
| `InteractionRule` | A rule defining what happens when placed pieces/cells satisfy a condition (e.g. a full row clears; matching tiles merge). |
| `ProgressionRule` | Governs pacing — time, speed, difficulty curve across a session. |
| `ScoringRule` | Converts resolved actions into score, including combo/chain multipliers. |
| `RewardRule` | Converts score/milestones into player-facing value — currency, unlocks, streak bonuses. |
| `GenerationRule` | Decides which piece is supplied next, plus the PieceSource's slot count and refill policy. |
| `Modifier` | A temporary, possibly stackable adjustment to one or more mechanic rule parameters (e.g. a score multiplier active for N placements). |
| `PowerUp` | A discrete player-deployed action — Undo, Piece Swap, Line Bomb, etc. Some power-ups are implemented as Modifiers internally (Score Multiplier); others directly mutate the board or tray. |
| `DropEvent` | An event emitted by the engine at steps 4 or 5 when a trigger condition is met, signalling that the Shell should award the player a power-up token. The engine does not manage inventories — it only emits the event. |
| `GameDefinition` | The authored config (board config + piece set + mechanic module) that instantiates one shippable game. |

---

## Engine Architecture — Core Loop

Every player action passes through the same **six-step loop**, regardless of which game is running. The mechanic module is invoked at steps marked ★ — everywhere else the loop is identical across the catalog.

| Step | Name | Description |
|---|---|---|
| 1 | **Input** | Player drags a piece from the source and releases it over the board. |
| 2★ | **Placement Validator** | Checks piece cells against the board using the mechanic's *Placement Rules* (overlap, bounds, blocked cells). Invalid → piece returns to source. |
| 3 | **Board Mutation** | Valid placement commits: piece cells write into the board grid. Piece transitions from `held` → `placed`. |
| 4★ | **Interaction Resolver** | The mechanic's *Interaction Rules* scan the mutated board and resolve outcomes — clears, merges, dissolves. This step defines "what game this is." The resolver may also emit `DropEvent`s when interaction outcomes meet defined trigger conditions (e.g. simultaneous multi-line clear → Line Bomb drop). |
| 5 | **Mechanic Hooks** | Scoring, Progression, and Reward rules run off the resolver's output — points awarded, timers/speed adjusted, rewards granted, win/loss checked. Active Modifiers apply here, adjusting rule inputs/outputs before they commit. The `ScoringRule` and `RewardRule` may also emit `DropEvent`s when score milestones are crossed (e.g. reaching 1000 points in a session → Wildcard drop). |
| 6★ | **State Broadcast & Replenish** | Board, piece source, and mechanic state changes are emitted. If the source has open slots, the mechanic's *Generation Rule* supplies the next piece(s) before the presentation layer renders the new frame. |

> **Key constraint:** The engine core never hardcodes clear/merge/escape/supply logic. It only calls out to the mechanic module's hook functions at steps 2, 4, and 6. Everything else is shared plumbing.

---

## Game Board

The board is the play surface for a single session. It is initialised from a `LevelConfig` at session start and does not change during the session. The engine is agnostic to shape, dimensions, and any zone logic.

### GridShape

A `GridShape` defines which cell coordinates exist on the board. Two concrete types:

| Type | Usage |
|---|---|
| `Rectangular(width, height)` | All cells in the W×H rectangle are valid. The default for most games. |
| `CellSet(coordinates)` | An explicit list of (row, col) coordinates. Used for irregular shapes — a hex outline, an L-shape, an octagon approximated on a square grid, etc. |

**Absent cells vs blocked cells** — this distinction is important:

- **Absent** — a coordinate outside the `GridShape`. Does not exist in board state. The engine, `PlacementRule`, and `InteractionRule` never see it.
- **Blocked** — a coordinate inside the `GridShape` but marked as an obstacle. Exists in board state; visible to the `InteractionRule`; can be rendered. Never placeable. Whether blocked cells count toward a line-fill condition is an `InteractionRule` decision, not a board decision.

A `PieceShape`'s offsets are validated against existing cells. Any placement that would require a cell outside the `GridShape` is unconditionally invalid — the `PlacementRule` is not consulted for absent cells.

### Cell States

| State | Meaning |
|---|---|
| `empty` | Exists within `GridShape`; unoccupied and placeable |
| `occupied` | Holds a resolved piece's cell |
| `blocked` | Exists within `GridShape`; obstacle, never placeable |
| `marked` | Transient placement preview |

### Zones

A `Zone` is a named collection of cell coordinates within the board, defined in `LevelConfig`. Zones are a mechanic concept — the board stores them as metadata but draws no conclusions from them. The `InteractionRule` reads zones and decides what "zone complete" means and what happens when a zone is filled.

Zones may overlap. Whether overlapping zones clear independently or together is an `InteractionRule` decision.

**Example — Woodoku-style 9×9:**  
Nine zones, each a 3×3 sub-grid. The `InteractionRule` clears any zone whose nine cells are all occupied, in addition to any fully-occupied rows or columns.

### LevelConfig

Every session is initialised from a `LevelConfig`:

```
LevelConfig {
  shape:        GridShape            // which cells exist
  blockedCells: [(row, col), ...]    // static obstacles within the shape
  zones:        [Zone, ...]          // named sub-regions; empty list if unused
}
```

For staged games, the `GameDefinition` contains a `LevelSequence` — an ordered list of `LevelConfig`s. The `ProgressionRule` advances through the sequence as the player completes each level. For endless games, a single `LevelConfig` (or a `LevelGenerator` that produces configs procedurally) applies to every session.

---

## Game Piece

A piece is **data plus state** — it has no knowledge of what a valid placement or a resolved interaction means. Only the mechanic module interprets pieces; the piece itself just carries shape and lifecycle.

**Piece Shape** — a set of relative cell offsets (a small matrix), stamped onto the board at a target origin cell.

**Piece State lifecycle:**
```
in-source → held → preview (valid/invalid) → placed → resolved / discarded
```

**Manipulation actions** available while a piece is held: drag, rotate, cancel (return to source). Individual `GameDefinition`s opt into which actions are available — e.g. a mechanic may disable rotation entirely.

---

## Game Mechanic — the Differentiator Layer

A mechanic module is composed of **seven independently swappable rule-sets**. Together they are the entire surface area a new game needs to define — nothing about the board or piece model changes between games.

| Rule-set | Governs |
|---|---|
| `Placement` | What makes a placement legal — overlap, board edges, blocked cells, special zone requirements. |
| `Interaction` | What happens once a placement resolves — clear full lines, merge matching tiles, dissolve enclosed shapes. The primary axis of difference between games. |
| `Progression` | Pacing within a session (turn-based vs. real-time, timers, speed/difficulty curve) AND level sequencing between sessions. For staged games, supplies the `LevelConfig` for each session and signals to the Shell whether the completed session was a level clear, a game over, or the final level. |
| `Scoring` | How interactions convert to points; combo and chain multipliers. |
| `Reward` | How score and milestones convert to player-facing value — currency, unlocks, streak bonuses, meta-progression. |
| `Win / Loss` | Terminal conditions — board-full loss, target-score win, or endless (no terminal state). |
| `Generation` | Which piece appears next (random-bag, weighted deck, difficulty-adaptive); how many slots the PieceSource holds (tray size); refill policy (per-slot trickle vs. batch refill when empty). |

> **Design note:** `PieceSource` is deliberately dumb — a slot container. What fills those slots is a mechanic concern governed by the Generation rule. This keeps structural entities free of rule logic.

---

## Power-Ups and Modifiers

These are related but distinct concepts. Both are mechanic concerns. Neither is a board entity or a piece property.

### Modifiers

A `Modifier` is a **temporary, runtime adjustment** to one or more mechanic rule parameters — active for a duration, then expired.

| Property | Definition |
|---|---|
| `target` | Which rule parameter(s) it adjusts — e.g. `progression.speedMultiplier`, `scoring.multiplier`, `interaction.autoClear`. |
| `duration` | Time-boxed, count-boxed (N placements/turns), or session-permanent. |
| `stacking` | How multiple active modifiers combine: additive, multiplicative, duration-refresh, or mutually exclusive (highest-priority wins). Configured per modifier type, not engine-wide. |

**Example:** A Score Multiplier modifier (`scoring.multiplier ×2`, 3 placements) doubles all score for the next three placements then expires automatically.

Modifiers are **optional per `GameDefinition`**. The simplest configuration has none active.

### Power-Ups

A `PowerUp` is a **discrete player-deployed action** drawn from inventory. Some power-ups (e.g. Score Multiplier) are implemented internally as Modifiers. Others act directly on the board or tray (e.g. Line Bomb, Piece Swap) without going through the Modifier system.

Power-ups are awarded to the player in two ways:

- **In-game drop** — the engine emits a `DropEvent` at steps 4 or 5 when a mechanic-defined trigger condition is met. The Shell converts this into a session-inventory token flagged `IN_GAME`. The engine does not manage inventories.
- **Booster Pack purchase** — the player buys a pack via the Shell's store. Tokens are added to persistent inventory flagged `PURCHASED`.

The source flag (`IN_GAME` vs `PURCHASED`) determines augmentation status when the token is deployed. The power-up's effect on the board and score is identical regardless of source. Full scoring rules — including multi-line clear bonuses — apply to any board state produced by a power-up, exactly as they would to any naturally produced state.

Each `GameDefinition` defines its own drop trigger table — which power-ups can drop and under what conditions. Power-up inventories are scoped per `GameDefinition`; tokens do not transfer between games.

See `PHOENIX_REWARDS_AND_AUGMENTATION.md` for the full power-up catalogue, drop trigger table, augmentation rules, and badge system.

---

## Game Definition — Authoring a White-Labeled Game

A new game is *authored*, not engineered. A `GameDefinition` is a config bundle that selects and parameterizes rule-sets. No engine code changes required.

```js
GameDefinition {

  // For an endless game: one LevelConfig used for every session.
  // For a staged game: an ordered LevelSequence; ProgressionRule advances through it.
  levels: [
    LevelConfig {
      shape:        Rectangular(8, 8)
      blockedCells: []
      zones:        []
    }
    // Additional LevelConfigs here for staged games, e.g.:
    // LevelConfig { shape: Rectangular(8,8), blockedCells: [(3,3),(4,4)], zones: [] }
    // LevelConfig { shape: CellSet([...hexOutlineCoords]), blockedCells: [], zones: [] }
  ]

  pieces:   { shapes: [...polyominoSet] }

  mechanic: {
    placement:   "noOverlap"
    interaction: "clearFullLines"
    progression: { type: "turnBased_noTimer", levelMode: "endless" }
                 // levelMode: "endless" | "staged" | "generated"
    scoring:     "cellsPlaced + linesCleared*100 * comboMultiplier"
    reward:      "currencyPerClear"
    winLoss:     "boardFullIsLoss"
    generation:  { policy: "randomBag", traySize: 3, refill: "whenEmpty_batch" }
    modifiers:   []
    drops: [
      { trigger: "lineCleared",             powerUp: "PieceSwap"      },
      { trigger: "scoreMilestone(500)",      powerUp: "Wildcard"       },
      { trigger: "simultaneousClears(2)",    powerUp: "ScoreMultiplier"},
      { trigger: "simultaneousClears(3)",    powerUp: "CellEraser"     },
      { trigger: "simultaneousClears(3)",    powerUp: "TrayRefresh"    },
      { trigger: "simultaneousClears(4)",    powerUp: "LineBomb"       },
      { trigger: "personalBestInSession",    powerUp: "Undo"           }
    ]
  }

  theme:    { skin: "ref://presentation-skins/example" }  // out of engine scope
}
```

> Presentation theming (skins, VFX, audio) is deliberately outside the engine's scope. It is a separate content pipeline that reads a `GameDefinition`'s `theme` block.

---

## Worked Example — Reference Mechanic: Grid-Fill Clear

The simplest possible `GameDefinition`, proving the engine reproduces a known genre through configuration alone (no engine changes).

| Concern | Configuration |
|---|---|
| Level sequence | Single `LevelConfig`; `levelMode: "endless"` — one session definition used indefinitely |
| Grid shape | `Rectangular(8, 8)` |
| Blocked cells | None |
| Zones | None |
| Pieces | Fixed library of small polyomino shapes (3–5 cells) |
| Generation | Random-bag draw from shape library; 3-slot tray; batch refill only once all three slots are empty |
| Placement | Piece cells must map onto empty cells only; piece must fit entirely within the `GridShape` |
| Interaction | Any row or column that becomes fully occupied clears (empties). Simultaneous multi-line clears resolve together. No zone logic. |
| Progression | Turn-based, no timer, no speed curve, endless (no level advancement) |
| Scoring | Points per cell placed + bonus per line cleared + multiplier for simultaneous multi-line clears |
| Reward | Score thresholds unlock cosmetic piece skins |
| Win / Loss | Loss when no piece in the tray can be legally placed anywhere on the board. No win condition — endless high-score loop. |
| Modifiers | None |
| Drops | Piece Swap on any line clear; Wildcard at score milestones; Score Multiplier on 2-line simultaneous clear; Cell Eraser and Tray Refresh on 3-line; Line Bomb on 4-line; Undo on in-session personal best |

Every row maps directly onto a rule-set slot, a `LevelConfig` field, or a `GameDefinition` config field. No change to the engine's board model, piece model, or core loop was required.

---

## Out of Scope for This Spec

This spec covers engine architecture and the board/piece/mechanic contract only. Explicitly not covered:

- Presentation theming and VFX/audio pipeline
- Backend/meta systems (accounts, live-ops, IAP)
- Multiplayer/async play

Each warrants its own spec once the core engine contract is agreed.
