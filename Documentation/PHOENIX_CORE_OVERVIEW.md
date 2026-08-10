# Phoenix Core — Engine Implementation Overview
> v0.2 Draft · 2026-08-08 · Internal

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
| `GameBoard` | The grid the player plays on. Owns cell state; agnostic to grid shape. |
| `Cell` | One addressable board location. Holds occupancy plus optional tags (color, type, blocked). |
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

The board is a grid of addressable cells. Grid dimensions and shape (square, hex) are set per `GameDefinition`; the engine is agnostic to both.

### Cell States

| State | Meaning |
|---|---|
| `empty` | Unoccupied, placeable |
| `occupied` | Holds a resolved piece's cell |
| `blocked` | Obstacle, never placeable |
| `marked` | Transient placement preview |

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
| `Progression` | Pacing — turn-based vs. real-time, timers, speed/difficulty curve, session length. |
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
  board:    { grid: "8x8", blockedCells: [] }
  pieces:   { shapes: [...polyominoSet] }
  mechanic: {
    placement:   "noOverlap"
    interaction: "clearFullLines"
    progression: "turnBased_noTimer"
    scoring:     "cellsPlaced + linesCleared*100 * comboMultiplier"
    reward:      "currencyPerClear"
    winLoss:     "boardFullIsLoss"
    generation:  { policy: "randomBag", traySize: 3, refill: "whenEmpty_batch" }
    modifiers:   []  // optional — omit entirely for simplest config
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

| Rule-set | Configuration |
|---|---|
| Board | 8×8, no blocked cells |
| Pieces | Fixed library of small polyomino shapes (3–5 cells) |
| Generation | Random-bag draw from shape library; 3-slot tray; batch refill only once all three slots are empty |
| Placement | Piece cells must map onto empty cells only; piece must fit entirely on the board |
| Interaction | Any row or column that becomes fully occupied clears (empties). Simultaneous multi-line clears resolve together. |
| Progression | Turn-based, no timer, no speed curve |
| Scoring | Points per cell placed + bonus per line cleared + multiplier for simultaneous multi-line clears |
| Reward | Score thresholds unlock cosmetic piece skins |
| Win / Loss | Loss when no piece in the tray can be legally placed anywhere on the board. No win condition — endless high-score loop. |
| Modifiers | None |
| Drops | Piece Swap on any line clear; Wildcard at score milestones; Score Multiplier on 2-line simultaneous clear; Cell Eraser and Tray Refresh on 3-line; Line Bomb on 4-line; Undo on in-session personal best |

Every row maps directly onto a rule-set slot from §7 and a field from the §9 config. No change to the engine's board model, piece model, or core loop was required.

---

## Out of Scope for This Spec

This spec covers engine architecture and the board/piece/mechanic contract only. Explicitly not covered:

- Presentation theming and VFX/audio pipeline
- Backend/meta systems (accounts, live-ops, IAP)
- Multiplayer/async play

Each warrants its own spec once the core engine contract is agreed.
