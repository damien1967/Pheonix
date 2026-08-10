# Phoenix — Rewards, Power-Ups, and Augmentation
> v0.2 Draft · 2026-08-08 · Internal

---

## 1. Overview

This document defines three interconnected systems that sit above the game engine and within the Phoenix Shell:

- **Power-Up System** — temporary mechanical advantages a player can earn through play or purchase
- **Badge System** — permanent achievements earned through play, never purchasable
- **Augmentation** — the distinction between a session where a player used only what the game gave them, and one where they brought in externally-sourced advantages

These three systems are designed together because they are dependent: augmentation status is determined by power-up source, and some badges are awarded based on augmentation behaviour.

---

## 2. The Pure / Augmented Distinction

This is the design principle everything else builds on, so it is stated first.

### 2.1 Definitions

**Pure session:** The player uses only power-ups that dropped for them naturally during that session. The session score is submitted to the **Pure leaderboard**.

**Augmented session:** The player uses one or more power-ups drawn from their purchased inventory (a Booster Pack) at any point during the session. The session is flagged Augmented the moment the first purchased power-up is deployed. It cannot revert to Pure. The session score is submitted to the **Augmented leaderboard**.

The trigger is **source, not type**. A Line Bomb that dropped during the session is Pure. An identical Line Bomb drawn from a purchased Booster Pack is Augmented. The power-up behaves identically in both cases — full effect, no difference in what it does to the board or to the score.

### 2.2 Power-Up Effect is Unconditional

A power-up does exactly the same thing regardless of where it came from. A Line Bomb clears the row and the `InteractionRule` fires normally, awarding the full line-clear score bonus. A Score Multiplier doubles the next three placements' scores. There is no capability penalty for purchased power-ups, and no capability bonus for dropped ones.

**Source determines leaderboard routing. It does not affect what the power-up does.**

### 2.3 Why This Distinction Matters

Without it, leaderboards become meaningless. Separating Pure and Augmented boards respects both player populations without punishing either:

- Players who want unaided competition get a clean leaderboard
- Players who enjoy power-ups and are willing to pay get their own competitive space
- Mixed leaderboards remain available for players who don't care about the distinction

Neither mode is presented as superior. The game does not shame augmented players. The badge awarded for first augmentation (`First Boost`) is celebratory, not pejorative.

### 2.4 Leaderboard Modes

The GeoScoreboard integration supports three views at every geographic level:

| View | Contains |
|---|---|
| **Pure** | Only sessions where no purchased power-up was used |
| **Augmented** | Only sessions where at least one purchased power-up was used |
| **All** | Both, sorted by score regardless of augmentation status |

The default view is **Pure**. A player can switch to Augmented or All at any time.

### 2.5 Augmentation is Irreversible Per Session

Once a player deploys a purchased power-up, that session is Augmented permanently. The game does not ask for confirmation — the player chose to open their inventory and deploy. Unused purchased power-ups at session end return to persistent inventory unconsumed.

---

## 3. The Drop System

All power-ups can drop for a player during normal play. Drops are the primary way players encounter power-ups organically, and they are what keeps sessions Pure.

### 3.1 How Drops Work

The `InteractionRule` evaluation at step 4 of the core loop (and the scoring hooks at step 5) can emit **drop events** alongside score events. A drop event adds one power-up token to the session inventory, flagged as `IN_GAME`.

Drops are triggered by in-game conditions, not randomly. Each power-up has defined trigger conditions — specific things the player has to achieve to earn the drop. This keeps drops feeling like rewards for skill rather than luck.

### 3.2 Drop Triggers — Grid Fill

| Power-Up | Drop Trigger | Rarity |
|---|---|---|
| Piece Swap | Any line clear | Common |
| Wildcard | Score milestone (500, 1000, 2000… within a session) | Common |
| Undo | Achieving a personal-best score during the session | Uncommon |
| Score Multiplier | Simultaneous 2-line clear | Uncommon |
| Cell Eraser | Simultaneous 3-line clear | Rare |
| Tray Refresh | Simultaneous 3-line clear | Rare |
| Line Bomb | Simultaneous 4+ line clear | Very Rare |

"Rarity" describes how frequently the trigger condition tends to arise in normal play — it is a consequence of trigger difficulty, not a random roll. A player who engineers a 4-line simultaneous clear will always receive a Line Bomb drop. There is no RNG in drop resolution; the trigger either fires or it does not.

### 3.3 Session Inventory vs Persistent Inventory

**Session inventory** holds power-ups earned via drops during the current session. These are flagged `IN_GAME`. They exist only for the duration of the session — unused session power-ups are discarded at session end. They do not carry over.

**Persistent inventory** holds power-ups from purchased Booster Packs. These are flagged `PURCHASED`. They persist until used. Unused purchased power-ups are never lost — they sit in inventory waiting for the player to choose to use them.

Both inventories appear as one unified tray to the player. The source flag is tracked internally; the player does not manage two separate lists.

---

## 4. Power-Up System

### 4.1 Scope

Power-ups are **game-specific**. The Grid Fill inventory is separate from any other Phoenix game's inventory. Power-ups are designed around a specific mechanic and have no coherent meaning outside it.

### 4.2 Power-Up Lifecycle

```
In-game drop (trigger condition met)
    └──→ Added to session inventory [IN_GAME]
              └──→ Deployed during session
                        └──→ Full effect applied
                        └──→ Score updated normally
                        └──→ Session remains Pure

Purchased Booster Pack
    └──→ Added to persistent inventory [PURCHASED]
              └──→ Player deploys during session
                        └──→ Full effect applied
                        └──→ Score updated normally
                        └──→ Session flagged Augmented
```

---

## 5. Grid Fill — Power-Up Catalogue

### 5.1 Undo
**What it does:** Reverses the last piece placement. The piece returns to the tray in the same slot. The board reverts. All score and rule effects from that placement are also reversed.

**Strategic use:** Recovers a misplaced piece. Highest value when a placement has blocked a near-complete row or column.

**Exploitation ceiling:** Covers exactly one step back. Cannot chain. Power-ups consumed during the undone placement are not restored.

**Drop trigger:** Achieving a personal-best score during the session — a reward for playing well enough to set a new best.

---

### 5.2 Piece Swap
**What it does:** Discards one piece from the tray and replaces it immediately with a freshly generated piece. The discarded piece is gone permanently.

**Strategic use:** Removes an awkward piece that has no useful placement, buying time and reshaping options.

**Exploitation ceiling:** The replacement is generated by the same `GenerationRule` as normal pieces — the player is trading a known problem for an unknown replacement.

**Drop trigger:** Any line clear. The most common drop; a reward for regular play.

---

### 5.3 Wildcard
**What it does:** Adds one 1×1 single-cell piece to the tray as a temporary extra slot (4 slots for one placement). Can be placed on any single empty cell.

**Strategic use:** Completes a near-full row or column that is one cell short, triggering a line clear that would not otherwise be possible.

**Exploitation ceiling:** One cell only. Does not complete a line by itself — it fills a gap. Value is entirely situational and requires the player to be in a position to use it well.

**Drop trigger:** Score milestones within a session (500, 1000, 2000, 5000 points). One drop per milestone, not repeating.

---

### 5.4 Cell Eraser
**What it does:** Removes one occupied cell from the board, returning it to empty. The player selects the target cell.

**Strategic use:** Breaks up a blocking cluster. Can open a route to a line clear that was otherwise blocked. The vacated cell also affects any `InteractionRule` evaluation that fires on the next placement — if removing the cell creates a completed line, the clear is evaluated and scored normally.

**Exploitation ceiling:** One cell per use. Cannot target blocked (obstacle) cells. The resulting empty cell must be in a useful position to have value — poor use wastes the token.

**Drop trigger:** Simultaneous 3-line clear.

---

### 5.5 Line Bomb
**What it does:** Clears one full row or column of the player's choice, regardless of occupancy. All cells in that line become empty. The clear is treated as a natural line clear — the `InteractionRule` fires, the full scoring bonus applies, and any multi-line multiplier is calculated normally.

**Strategic use:** Emergency pressure relief when the board is filling dangerously. Also effective as a deliberate setup move — clear a line to enable a chain of subsequent natural clears.

**Exploitation ceiling:** One line per use. Value is high but the player must choose the right line. A poorly targeted Line Bomb relieves pressure without enabling follow-up clears.

**Drop trigger:** Simultaneous 4+ line clear. The hardest drop to earn; high-skill reward.

---

### 5.6 Tray Refresh
**What it does:** Discards all three pieces in the current tray and draws three fresh pieces. The discarded pieces are gone.

**Strategic use:** When all three current pieces are genuinely unplayable and the player wants to avoid multiple individual Swaps.

**Exploitation ceiling:** The replacement pieces are generated normally by the `GenerationRule` — no guarantee of a better set. The player is discarding known pieces for an unknown hand.

**Drop trigger:** Simultaneous 3-line clear (alongside Cell Eraser — both can drop from the same trigger event).

---

### 5.7 Score Multiplier
**What it does:** Doubles all score from the next 3 placements — cells placed, line clears triggered, and multi-line multipliers all calculate at double value for those 3 placements.

**Strategic use:** Held until the player can engineer a high-value placement, ideally one that triggers multiple simultaneous line clears. Skill-dependent — a multiplier applied to a low-scoring board state yields proportionally less.

**Exploitation ceiling:** 3 placements only. The clock starts on deployment, not on scoring — a placement that scores nothing still consumes one of the three uses. The player must be ready to score well before deploying.

**Drop trigger:** Simultaneous 2-line clear. Relatively achievable, making Score Multiplier a meaningful in-game reward for competent play.

---

## 6. Booster Packs — Monetisation Model

Booster Packs are **one-time purchases**. The player buys a named pack at a fixed price and receives a defined set of power-up uses added to their persistent inventory. No subscription, no premium currency, no rotating stock.

### 6.1 Design Principles

- The player knows exactly what they are buying before they pay.
- Each pack can be purchased once. Purchasing the same pack again is blocked — the player must choose a different pack. This prevents unlimited spending on a single item.
- No pack is necessary to enjoy the game. The game is complete without any purchase.
- Purchased power-ups behave identically to dropped ones — full effect, every time. Players are buying access, not a superior product.
- No pack confers any advantage on the Pure leaderboard — purchased tokens always flag the session as Augmented.

### 6.2 Suggested Pack Structure — Grid Fill

| Pack | Contents | Suggested Price |
|---|---|---|
| **Starter Pack** | 5× Piece Swap, 5× Wildcard, 3× Undo | £1.99 |
| **Tactician Pack** | 5× Undo, 5× Wildcard, 3× Cell Eraser, 3× Score Multiplier | £2.99 |
| **Power Pack** | 5× Cell Eraser, 3× Line Bomb, 3× Tray Refresh, 3× Score Multiplier | £4.99 |
| **Complete Pack** | All of the above combined, best per-unit value | £7.99 |

The Complete Pack is presented last and offered as the best value option for players who know they enjoy power-ups — not as a primary upsell. No artificial urgency, no limited-time pressure.

### 6.3 What Is Never for Sale

- Badges
- Leaderboard position
- Score directly
- A Pure session flag

---

## 7. Badge System

Badges are **permanent, earned-only achievements**. They cannot be purchased, gifted, or transferred.

### 7.1 Badge Categories

| Category | Scope | Description |
|---|---|---|
| **Game Skill** | Per `GameDefinition` | Earned through specific in-game achievements |
| **Progression** | Per `GameDefinition` | Earned through volume and consistency of play |
| **Phoenix Meta** | Platform-wide | Earned across the Phoenix ecosystem |
| **Augmentation** | Platform-wide | Earned through specific augmentation milestones |

### 7.2 Grid Fill — Game Skill Badges

| Badge | Trigger |
|---|---|
| **First Line** | Clear your first row or column |
| **Double** | Clear 2 lines simultaneously |
| **Triple** | Clear 3 lines simultaneously |
| **Grand Slam** | Clear 4+ lines simultaneously |
| **Clean Sweep** | Clear 4+ lines simultaneously without any power-up active |
| **Minimalist** | Score 500+ in a session using only pieces of 3 cells or fewer |
| **Unassisted** | Score 1000+ in a session without using any power-up (dropped or purchased) |
| **Speed Run** | Reach 1000 points in under 5 minutes |

### 7.3 Grid Fill — Progression Badges

| Badge | Trigger |
|---|---|
| **Centurion** | Score 100+ in a single session |
| **Millennium** | Score 1000+ in a single session |
| **Ten Thousand** | Score 10,000+ in a single session |
| **Frequent Player** | Complete 50 Grid Fill sessions |
| **Veteran** | Complete 500 Grid Fill sessions |
| **Daily Habit** | Play Grid Fill every day for 7 consecutive days |
| **Tray Master** | Empty the full tray 50 times across all sessions |

### 7.4 Phoenix Meta Badges

| Badge | Trigger |
|---|---|
| **Pioneer** | Play your first Phoenix game |
| **Explorer** | Play two different Phoenix games |
| **Collector** | Earn at least one Game Skill badge in 3 different Phoenix games |
| **Leaderboard Entry** | Appear on any GeoScoreboard for the first time |
| **Local Hero** | Reach top 10 in your district on any GeoScoreboard |
| **Neighbourhood Champion** | Reach #1 in your locality on any GeoScoreboard |
| **Purist** | Complete 25 Pure sessions across any Phoenix games |
| **Iron Will** | Complete 100 Pure sessions across any Phoenix games |
| **Dedicated** | Play 500 sessions total across all Phoenix games |

### 7.5 Augmentation Badges

Earned through augmentation milestones. Celebratory, not commercial.

| Badge | Trigger | Notes |
|---|---|---|
| **First Boost** | Use your first purchased power-up in any session | Earned once, platform-wide |
| **Augmented** | Complete your first Augmented session | First full augmented run |
| **Power Player** | Score in the top 10% of the Augmented leaderboard | Skill + augmentation |
| **Pure at Heart** | Earn `First Boost` then complete 50 subsequent Pure sessions | Tried augmentation; chose not to continue |

`Pure at Heart` is deliberate. It rewards players who sampled augmentation and returned to pure play. It is not a judgement — it is an acknowledgement.

---

## 8. Architecture Notes for Developers

### 8.1 Power-Up Token

```kotlin
data class PowerUpToken(
    val type: PowerUpType,
    val source: PowerUpSource,     // IN_GAME or PURCHASED
    val gameDefinitionId: String
)

enum class PowerUpSource { IN_GAME, PURCHASED }
```

Source is set at creation and immutable. It is read at deployment time by the augmentation tracker. It is never surfaced to the player directly.

### 8.2 Drop Evaluation

Drop events are emitted by the `InteractionRule` evaluation (step 4) and the scoring hooks (step 5). Each `GameDefinition`'s mechanic defines its own drop trigger table. The engine emits a `DropEvent` alongside any `ScoreEvent`; the Shell's drop handler converts it into a `PowerUpToken` with source `IN_GAME` and adds it to the session inventory.

The engine does not manage inventories. It emits events. The Shell handles the rest.

### 8.3 Power-Up Effect and Scoring

When any power-up fires — regardless of source — the engine processes the resulting board state through the normal rule pipeline. A Line Bomb clears the row; the `InteractionRule` evaluates the result; the `ScoringRule` applies the full line-clear bonus including any multi-line multiplier. There is no flag passed to the engine indicating whether the power-up was purchased or dropped. The engine does not know and does not need to know.

Augmentation tracking lives entirely in the Shell.

### 8.4 Augmentation State

```kotlin
data class SessionAugmentationStatus(
    val isAugmented: Boolean = false,
    val augmentedAtPlacement: Int? = null,
    val purchasedTokensUsed: Int = 0
)
```

Once `isAugmented` flips to `true` it cannot revert. Score submission carries this flag to GeoScoreboard for leaderboard routing.

### 8.5 Badge Evaluation

A `BadgeEvaluator` in the Shell reads `SessionResult` and current player history to determine newly earned badges. It runs after every session end, before navigation to the Score Screen. Badge state is persisted in `AppState.playerProgress`.

Cross-game (Phoenix Meta) badges are evaluated against the platform-wide player profile.

### 8.6 Leaderboard Submission

`SessionResult` carries:

```kotlin
data class SessionResult(
    val score: Int,
    val augmentationStatus: SessionAugmentationStatus,
    // ...
)
```

GeoScoreboard routes the score to Pure, Augmented, or both boards based on the augmentation flag. The Shell does not route scores manually — it submits the result and the flag; GeoScoreboard handles the rest.

---

## 9. Out of Scope for This Spec

- GeoScoreboard API changes required to support augmentation flagging (update `GeoScoreboard_System_Specification.md`)
- Social features: sharing badges, gifting power-ups
- Seasonal or time-limited badge events
- App Store / Play Store purchase implementation
- Power-up catalogues for GameDefinitions other than Grid Fill
