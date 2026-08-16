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
| **Done.** `GameDefinition` gains `levelMode: LevelMode` (`ENDLESS`/`STAGED`/`GENERATED`) and `drops: List<DropTrigger>`. Each `DropTrigger` pairs a `DropCondition` with a `DropOutcome` (`Deterministic` — every listed power-up fires, or `Weighted` — one roll against odds that need not sum to 100, the shortfall being an implicit no-drop chance, reserved for conditions that are already hard to achieve). Each `DropTrigger` is self-contained by construction — no cross-trigger references — so the table has no ordering dependency and can't form a cycle between triggers. `DropCondition` is split into `Engine` (`AnyLineCleared`, `SimultaneousClears(n)` — cyclic, stateless per-placement checks that can refire any number of times; `ScoreMilestones(thresholds)` — sequential, one ordered list, each threshold fires at most once) and `Shell` (`PersonalBestInSession`). The split matters: `PersonalBestInSession` means beating the player's all-time best from *previous* sessions, not a same-session high (score only rises within a session, so a same-session reading would fire almost every placement) — that needs cross-session player history, which per rule 8 and `PHOENIX_REWARDS_AND_AUGMENTATION.md` §8.4-8.6 lives in the Shell, not the engine. `Engine` conditions are decided from this session's own board/score and could in principle be checked by an `InteractionRule`/`ScoringRule` directly; `Shell` conditions never can be. Caught and fixed in review before anything consumed the unsplit type. Scoped to the data model only: `GridFillClearInteractionRule` (#61) keeps its own hardcoded trigger table for now rather than reading this one generically — wiring a mechanic's rules to consume `drops` at runtime is a separate, larger follow-up (a generic trigger-evaluation engine, `Engine` conditions only), not part of this ticket. Odds/threshold validation deferred to #59. | [#58](https://github.com/damien1967/Pheonix/issues/58) (closed) | — |
| `GameDefinitionValidator` needs new failure modes once the above land (empty level sequence, etc.) | [#59](https://github.com/damien1967/Pheonix/issues/59) | P1 (depends on #57, #58) |

### #7 — GridFillClearPlacementRule (closed — rework done)

| Gap | Issue | Priority |
|---|---|---|
| **Done.** Checked `board.rowCount`/`board.columnCount` directly instead of querying cell existence through `GridShape`. Now validates via `board.shape.contains(position)`. | [#60](https://github.com/damien1967/Pheonix/issues/60) (closed) | — |

### #8 — GridFillClearInteractionRule (closed — rework done)

| Gap | Issue | Priority |
|---|---|---|
| **Done.** Implements the reference mechanic's own drop trigger table (Piece Swap, Score Multiplier, Cell Eraser, Tray Refresh, Line Bomb — `PHOENIX_REWARDS_AND_AUGMENTATION.md` §3.2). `resolve()` computes simultaneous line count and emits the matching `DropEvent`s, stacking rather than mutually exclusive. | [#61](https://github.com/damien1967/Pheonix/issues/61) (closed) | — |

All 4 sub-issues of #8 are closed; #8 itself is closed.

### #9, #10 — #9 done, #10 not yet started

Flagged rather than described as debt, since there's no shipped code yet to drift — but both would currently be built against interfaces that are already known to be wrong:

| Ticket | Issue | Priority |
|---|---|---|
| **Resolved.** #9 (`GridFillClearProgressionRule`) was blocked pending `ProgressionRule`'s redesign; #53 landed, #9 was implemented and closed. | [#62](https://github.com/damien1967/Pheonix/issues/62) (closed) | — |
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

**Remaining open debt:** #59, #63 — both P1. #58 is closed, so #59 is now unblocked too. Neither is a P0 root; each can be picked up independently.

**Per the standing instruction:** the P0 root debt is addressed. #9 (`GridFillClearProgressionRule`) is confirmed unblocked (#62). Phase 3 ticket work can resume; check each ticket's own spec section against `TECHNICAL_DEBT.md` before starting, per the standing rule above.
