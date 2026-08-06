# Phoenix Core — Engine Implementation Overview
> v0.1 Draft · 2026-07-11 · Internal

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
| `Modifier` | A temporary, possibly stackable adjustment to one or more mechanic rule parameters — a power-up. |
| `GameDefinition` | The authored config (board config + piece set + mechanic module) that instantiates one shippable game. |

---

## Engine Architecture — Core Loop

Every player action passes through the same **six-step loop**, regardless of which game is running. The mechanic module is invoked at steps marked ★ — everywhere else the loop is identical across the catalog.

| Step | Name | Description |
|---|---|---|
| 1 | **Input** | Player drags a piece from the source and releases it over the board. |
| 2★ | **Placement Validator** | Checks piece cells against the board using the mechanic's *Placement Rules* (overlap, bounds, blocked cells). Invalid → piece returns to source. |
| 3 | **Board Mutation** | Valid placement commits: piece cells write into the board grid. Piece transitions from `held` → `placed`. |
| 4★ | **Interaction Resolver** | The mechanic's *Interaction Rules* scan the mutated board and resolve outcomes — clears, merges, dissolves. This step defines "what game this is." |
| 5 | **Mechanic Hooks** | Scoring, Progression, and Reward rules run off the resolver's output — points awarded, timers/speed adjusted, rewards granted, win/loss checked. Active Modifiers (§8) apply here, adjusting rule inputs/outputs before they commit. |
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

## Modifiers — Power-Ups

A `Modifier` is a **temporary, runtime adjustment** to one or more mechanic rule parameters.

| Property | Definition |
|---|---|
| `trigger` | What grants it — a tagged piece resolving ("power piece"), an Interaction Rule side-effect (special clear pattern), or a Reward Rule payout (score-threshold unlock). |
| `target` | Which rule parameter(s) it adjusts — e.g. `progression.speedMultiplier`, `scoring.multiplier`, `interaction.autoClear`. |
| `duration` | Time-boxed, count-boxed (N placements/turns), or session-permanent. |
| `stacking` | How multiple active modifiers combine: additive, multiplicative, duration-refresh, or mutually exclusive (highest-priority wins). Configured per modifier type, not engine-wide. |

**Example:** A "Slow-Mo" modifier (`progression.speedMultiplier ×0.5`, 10s) and a "Double Points" modifier (`scoring.multiplier ×2`, 8s) stack additively and independently — the board runs slower while every clear scores double.

Modifiers are **optional per `GameDefinition`**. The simplest configuration has none active.

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

Every row maps directly onto a rule-set slot from §7 and a field from the §9 config. No change to the engine's board model, piece model, or core loop was required.

---

## Out of Scope for This Spec

This spec covers engine architecture and the board/piece/mechanic contract only. Explicitly not covered:

- Presentation theming and VFX/audio pipeline
- Backend/meta systems (accounts, live-ops, IAP)
- Multiplayer/async play

Each warrants its own spec once the core engine contract is agreed.
