# Phoenix — Technical Debt
> v1.0 · 2026-08-11 · Internal

## How This Came About

Issues #1–8 were implemented and shipped against `PHOENIX_CORE_OVERVIEW.md` v0.1 and `PHOENIX_SHELL_OVERVIEW.md` v0.1. Both documents have since moved to v0.3, adding non-rectangular grids (`GridShape`), zones, staged levels (`LevelConfig`/`LevelSequence`), and an entire power-up/drop/augmentation system (`DropEvent`, `PowerUpToken`, `PHOENIX_REWARDS_AND_AUGMENTATION.md`). This document is the audit of what the shipped code and open tickets now assume that the specs no longer say, and what the specs now say that nothing implements yet.

**Method:** every current spec document was re-read in full and compared line by line against the actual implementation in `shared/src/commonMain/kotlin/phoenix/` and the descriptions on issues #1–14 and their sub-issues.

**Standing rule going forward:** before starting any new ticket, check whether the spec section it's built from has changed since the ticket was written. This drift happened because three documents were edited in parallel while implementation continued against the versions open at ticket-creation time — there was no signal that they'd moved.

---

## Findings by Ticket

### #1 — GameBoard and Cell (closed — all rework done)

| Gap | Issue | Priority |
|---|---|---|
| `GameBoard` hardcodes `rowCount`/`columnCount`; spec now requires a `GridShape` type (`Rectangular` or `CellSet`) — `CLAUDE.md` rule 6: "the `GameBoard` does not know what shape it is" | [#50](https://github.com/damien1967/Pheonix/issues/50) (closed) | P0 |
| **Resolved as a side effect of #50, no separate implementation needed.** `GameBoard.create()` only builds `Cell` entries for `shape.positions`; `cellAt()`/`withCell()` already throw for any position outside the shape (already covered by `GameBoardTest.kt`). Blocked cells exist in board state with `CellState.BLOCKED`. Per spec, `absent` was never meant to be a `CellState` value — it's the absence of an entry, which this already is. | [#51](https://github.com/damien1967/Pheonix/issues/51) (closed) | — |
| **Done.** `GameBoard` gains `zones: List<Zone>`, populated via `GameBoard.create()` — storage only, per rule 2. `LevelConfig` also gains the `zones` field #57 deferred. | [#52](https://github.com/damien1967/Pheonix/issues/52) (closed) | — |

All 11 sub-issues of #1 (original implementation + #50/#51/#52 rework) are closed; #1 itself is closed.

### #5 — GameMechanic and seven rule-set interfaces (shipped, needs rework)

| Gap | Issue | Priority |
|---|---|---|
| **Done.** `ProgressionRule` gains `nextLevel()` and `levelOutcome()` alongside `speedMultiplierAtTurn()` — supplies the next `LevelConfig` and signals level-clear/game-over/final-level, implementing the #56 mapping. Required #57 (`LevelConfig`/`LevelSequence`) as a prerequisite, done first once noticed. Unblocks #9/#62. | [#53](https://github.com/damien1967/Pheonix/issues/53) (closed) | P0 |
| **Done.** `DropEvent(val powerUpId: String)` added, mirroring `Reward(val id: String)`'s existing pattern rather than a closed `PowerUpType` enum (that catalogue is game-specific, and rule 3 rules out engine changes per game). `InteractionResult` gains `drops: List<DropEvent> = emptyList()`. Unblocks #55, #61, #63. | [#54](https://github.com/damien1967/Pheonix/issues/54) (closed) | P0 |
| **Done.** `ScoringRule.score()` returns `ScoringResult(score, drops)`; `RewardRule.rewardsEarnedAt()` returns `RewardResult(rewards, drops)` — both mirroring `InteractionResult`'s pattern from #54. No concrete implementation existed yet (#10/#11 not started), so only the noOp test stubs needed fixing. Unblocks #63. | [#55](https://github.com/damien1967/Pheonix/issues/55) (closed) | P1 |
| **Resolved design question:** `SessionOutcome` (`Ongoing`/`Won`/`Lost`) doesn't map onto the Shell spec's four-way `levelOutcome` (`LEVEL_CLEARED`/`GAME_OVER`/`FINAL_LEVEL_CLEARED`/`ENDLESS_ENDED`). Resolved: `ProgressionRule` computes `levelOutcome` by combining `WinLossRule`'s result with level-sequence state (does a next `LevelConfig` exist, is this endless); `WinLossRule` itself is unchanged. See #56 for the full mapping — now a concrete input to #53. | [#56](https://github.com/damien1967/Pheonix/issues/56) (closed) | P1 |

### #6 — GameDefinition and validation (shipped, needs rework)

| Gap | Issue | Priority |
|---|---|---|
| **Done.** `BoardConfig` replaced with `LevelConfig` (`GridShape` + `blockedCells`); `GameDefinition.board` (singular) replaced with `GameDefinition.levels: LevelSequence` — `CLAUDE.md` rule 7. `zones` deferred to #52 (Zone metadata storage doesn't exist yet). Turned out to be a real prerequisite for #53 (ProgressionRule can't supply a `LevelConfig` that doesn't exist) — done first, ahead of the original #50→#53→#54 remediation order below. | [#57](https://github.com/damien1967/Pheonix/issues/57) (closed) | P1 (depended on #50) |
| No `levelMode` (`endless`/`staged`/`generated`), no `drops` trigger table on `GameDefinition` | [#58](https://github.com/damien1967/Pheonix/issues/58) | P1 |
| `GameDefinitionValidator` needs new failure modes once the above land (empty level sequence, etc.) | [#59](https://github.com/damien1967/Pheonix/issues/59) | P1 (depends on #57, #58) |

### #7 — GridFillClearPlacementRule (shipped, minor rework)

| Gap | Issue | Priority |
|---|---|---|
| Checks `board.rowCount`/`board.columnCount` directly instead of querying cell existence through `GridShape`. **Functionally correct today** — Grid-Fill Clear is `Rectangular(8,8)` with no absent cells — but will silently misbehave on any non-rectangular `GameDefinition` once one exists | [#60](https://github.com/damien1967/Pheonix/issues/60) | P1 (depends on #50) |

### #8 — GridFillClearInteractionRule (shipped, needs addition)

| Gap | Issue | Priority |
|---|---|---|
| Doesn't implement the reference mechanic's own drop trigger table (Piece Swap, Score Multiplier, Cell Eraser, Tray Refresh, Line Bomb — `PHOENIX_REWARDS_AND_AUGMENTATION.md` §3.2). `DropEvent` now exists (#54) — unblocked | [#61](https://github.com/damien1967/Pheonix/issues/61) | P1 |

### #9, #10 — not yet started

Flagged rather than described as debt, since there's no shipped code yet to drift — but both would currently be built against interfaces that are already known to be wrong:

| Ticket | Issue | Priority |
|---|---|---|
| **Resolved.** #9 (`GridFillClearProgressionRule`) was blocked pending `ProgressionRule`'s redesign; #53 is done, #9 is unblocked. | [#62](https://github.com/damien1967/Pheonix/issues/62) (closed) | — |
| #10 (`GridFillClearScoringRule`) — needs milestone-drop emission support before its Wildcard trigger can be implemented — #55 done, now unblocked | [#63](https://github.com/damien1967/Pheonix/issues/63) | P1 |

### Clean sweep

#2 (`PieceShape`), #3 (`GamePiece`), #4 (`PieceSource`) — checked in full against the current specs. No changes affect these. No debt items filed.

### Noted but not ticketed

`Modifier` (`modifier/` package) isn't implemented and was never one of tickets #1–10's scope. The spec's `Modifier` target examples (`progression.speedMultiplier`, `scoring.multiplier`) already exist as real parameters on the current interfaces, so there's no known incompatibility — just nothing built yet. No urgency; not filed as debt.

---

## Recommended Remediation Order

The three P0 items are root dependencies — nothing else in this list can be correctly implemented before them:

1. **[#50](https://github.com/damien1967/Pheonix/issues/50)** GridShape — unblocks #51, #57, #60 (closed)
2. **[#53](https://github.com/damien1967/Pheonix/issues/53)** ProgressionRule redesign — unblocks #62, and informs #56 (closed)
3. **[#54](https://github.com/damien1967/Pheonix/issues/54)** DropEvent type — unblocks #55, #61, #63 (closed)

All three original P0 roots are closed. #56 (the `SessionOutcome`/`levelOutcome` open question) is resolved and implemented as part of #53. #54 was checked for the same kind of hidden dependency that #53 had (#57) before starting — it didn't have one, since `DropEvent` deliberately doesn't reference the not-yet-built `PowerUpType`.

**Correction found while starting #53:** this order didn't catch that #53 ("`ProgressionRule` supplies the next `LevelConfig`") had no type to supply until `LevelConfig`/`LevelSequence` existed — that's #57, not one of the three listed roots. **[#57](https://github.com/damien1967/Pheonix/issues/57) was done first** (closed), ahead of #53.

**Remaining open debt:** #58, #59, #60, #61, #63 — all P1, all unblocked now except #59 (depends on #58). None are P0 roots; each can be picked up independently.

**Per the standing instruction:** the P0 root debt is addressed. #9 (`GridFillClearProgressionRule`) is confirmed unblocked (#62). Phase 3 ticket work can resume; check each ticket's own spec section against `TECHNICAL_DEBT.md` before starting, per the standing rule above.
