# GeoScoreboard System Specification

**Version:** 1.2  
**Status:** Draft  
**Audience:** Game engine integrators, backend platform engineers  

---

## 1. Purpose and Vision

GeoScoreboard is a location-aware, privacy-first global ranking service designed to be embedded into any game engine. It answers the question every competitive player silently asks: *"Where do I stand — not just in the world, but in my world?"*

A score of 400 is abstract against a global pool of millions. Against a neighbourhood of 8 players it becomes intensely personal. GeoScoreboard surfaces both perspectives simultaneously, giving players meaningful targets at every scale — from the street they live on to the entire planet.

The system is built around two design principles:

- **Everyone competes globally by default.** No action required. No data surrendered.
- **Narrower competition requires explicit, informed consent.** Players who want local context trade only the minimum location data needed to place them at that level of the hierarchy.

---

## 2. Geographic Hierarchy

GeoScoreboard organises rankings into a fixed six-level geographic hierarchy. Each level is a distinct leaderboard. A score exists at every level the player has consented to participate in.

```
WORLD
 └── COUNTRY
      └── REGION          (state, county, province — varies by nation)
           └── CITY
                └── DISTRICT    (borough, neighbourhood, postcode area)
                     └── LOCALITY    (street, postcode sector)
```

A player's position at any given level is computed only against other players who have consented to that same level or narrower. A player who consents to City-level is visible in the City, District, and Locality boards only if they subsequently consent to those narrower levels.

### 2.1 Level Definitions

| Level | Example | Typical Population |
|---|---|---|
| World | — | All players |
| Country | United Kingdom | Millions |
| Region | West Midlands | Hundreds of thousands |
| City | Coventry | Tens of thousands |
| District | Ball Hill | Hundreds |
| Locality | Leighton Road | Tens |

Populations at District and below are expected to be small. The system surfaces player count alongside rank at these levels so a player understands the meaning of their position ("2nd of 2" versus "2nd of 2,000").

---

## 3. Player Identity

### 3.1 Identity Model

Every player has a **Player Record** consisting of:

- A permanent, opaque **Player ID** (system-generated, never exposed to other players)
- A **Display Name** — chosen by the player, shown on all boards they participate in
- A **Privacy Tier** — the narrowest geographic level the player consents to (see Section 4)
- **Score Records** — four time-windowed personal bests stored independently of leaderboard state: All-Time, This Year, This Week, Today (see Section 5.1)

Display Names are pseudonymous by design. The system never requires a real name. Players may use any handle they choose, subject to a content moderation policy defined by the integrating game.

### 3.2 Display Name Visibility

A player's Display Name is visible to other players **only on boards where the viewing player also participates**. A player who opts into World-only sees only anonymous entries on all boards they are not enrolled in. A player enrolled at City level sees Display Names of all other City-level (and narrower) participants on city and below boards.

This prevents location inference: a player enrolled only at World level cannot determine where a named player is located by observing which boards their name appears on.

### 3.3 Anonymous World Participants

Players who have not provided a Display Name, or who explicitly opt for anonymity, are assigned a system pseudonym (e.g. `Player #10482`) on the World board. They are indistinguishable from named players on that board except by their own game client, which knows their identity locally.

---

## 4. Privacy Model

### 4.1 Consent Tiers

Privacy is expressed as a single setting: the player's **Consent Tier**, which is the narrowest geographic level they are willing to be ranked at. Participation at a chosen tier automatically includes all broader tiers.

| Tier | Boards Enrolled | Location Data Required |
|---|---|---|
| 0 — World Only | World | None |
| 1 — Country | World, Country | Country (inferred, see 4.3) |
| 2 — Region | World → Region | Country + Region (inferred) |
| 3 — City | World → City | Country + Region + City (inferred) |
| 4 — District | World → District | Precise location (consented) |
| 5 — Locality | World → Locality | Precise location (consented) |

Default on first run is **Tier 0**. The game client must present a clear consent flow before enrolling a player at any higher tier. The system will refuse to enrol a player above their declared tier regardless of what location data is presented.

### 4.2 Location Data Retention Policy

- Tiers 0–3: Location is stored only as a **resolved geographic label** (e.g. "West Midlands / Coventry"). Raw coordinates or IP addresses are never persisted.
- Tiers 4–5: Precise location (GPS or equivalent) is resolved to a District/Locality label at the point of submission. Only the label is stored. Raw coordinates are discarded immediately after resolution.
- A player may reduce their Consent Tier at any time. When they do, their scores are removed from all boards narrower than the new tier and the corresponding location labels are deleted from their Player Record.

### 4.3 Location Inference Strategy

For Tiers 1–3, location is inferred rather than explicitly provided:

- **Primary:** Device locale + network-level country signal (IP geolocation to country/region)
- **Fallback:** Declared locale setting within the game client
- **Edge case:** If inference fails or is ambiguous (e.g. VPN detected), the player is placed at the broadest tier that can be confidently resolved. They are notified within the game client.

For Tiers 4–5, the game client must request explicit device location permission before submitting. The consent flow for these tiers must be distinct from the system-level location permission and must clearly explain that the player's approximate street-level area will be associated with their score.

---

## 5. Score Submission

### 5.1 What Constitutes a Score

GeoScoreboard is score-agnostic. The integrating game defines:

- The **score value** (integer or float)
- Whether **lower is better** (e.g. time-based scores) or **higher is better**
- The **score label** displayed to players (e.g. "points", "seconds", "metres")
- Whether a player may submit **multiple scores** per session or only their **personal best**

These are configured at game registration time and are immutable per game instance.

### 5.2 Time Windows

Every score submission is evaluated against four time windows. Each window maintains an independent personal best for the player and an independent ranking set on every board they are enrolled in:

| Window | Resets | Description |
|---|---|---|
| All Time | Never | The player's highest score ever. The primary ranking. |
| This Year | 1 January 00:00 UTC | Best score in the current calendar year. |
| This Week | Monday 00:00 UTC | Best score in the current ISO week. |
| Today | Midnight UTC | Best score in the current calendar day. |

On submission, the score is compared to the player's existing best in each window and replaces it if higher (or lower, for lower-is-better games). All four windows are updated in a single atomic operation. There is no concept of a "worst score" or rolling average; only the best score per window is tracked and ranked.

Time windows reset on schedule regardless of player activity. A player with no submission in the current day has no Today score; they do not appear on the Today board and their absence does not affect other players' ranks.

### 5.3 Submission Flow

```
Game Client
    │
    ▼
Score Submission Request
  - Player ID
  - Game ID
  - Score Value
  - Session Token (anti-cheat)
  - Location Signal (if Tier ≥ 4: precise; else: inferred)
  - Augmentation Status (PURE | AUGMENTED)
    │
    ▼
Score Ingestion Service
  ├── Validate session token
  ├── Validate score within plausible range (game-configured bounds)
  ├── Resolve location to geographic labels at all consented tiers
  ├── Read Augmentation Status from submission
  ├── Compare score to player's existing best in each time window × augmentation track
  │     ├── Update any windows where this score is a new best (Pure track or Augmented track)
  │     └── Flag All-Time PB if applicable, per track (shown prominently on score screen)
  └── Emit score event to Ranking Engine (carries window update flags + augmentation track)
    │
    ▼
Ranking Engine
  └── Updates player's position on all boards × all time windows they are enrolled in
```

Score submission is **asynchronous from the player's perspective**. The game client receives an immediate acknowledgement with the player's pre-existing rank and personal best. Updated board positions are delivered as a separate response within a short window (target: under 2 seconds at 95th percentile).

### 5.4 Score Plausibility and Anti-Cheat

The system is not a full anti-cheat solution but applies basic guards:

- Score values outside game-configured min/max bounds are rejected outright.
- Score submissions without a valid server-issued session token are rejected.
- Anomaly detection flags scores that represent statistically implausible improvement (e.g. a player jumping from bottom 10% to top 0.01% in a single session). Flagged scores are held for review and the player's displayed rank is frozen at their last verified position during review.
- The integrating game is responsible for any gameplay-level anti-cheat. The scoreboard trusts scores that clear the above gates.

---

## 6. Leaderboard Views

### 6.1 View Types

The system provides two view modes and three leaderboard tracks, all available at every geographic level:

**Contextual View (default)**
Shows the player's own rank with a window of competitors immediately above and below them. Recommended window: 5 above, 5 below. This is the primary view — it gives players named targets to pursue without overwhelming them with irrelevant entries.

**Top-N View**
Shows the highest-ranked N players at a given level. Recommended default: top 100. The player's own entry is always appended below the list if they fall outside the top N, so they always see their rank in context.

**Leaderboard Tracks**

Every board at every geographic level maintains three parallel ranking sets:

| Track | Contains | Default |
|---|---|---|
| **Pure** | Only sessions submitted with `PURE` status | Yes — shown by default |
| **Augmented** | Only sessions submitted with `AUGMENTED` status | No |
| **All** | Both Pure and Augmented, ranked together | No |

The player switches between tracks via a toggle on the leaderboard view. Their own entry always shows their status (`Pure` / `Augmented`) regardless of which track is displayed. A player's personal best is tracked independently per track — a Pure PB and an Augmented PB are separate records.

The Ranking Engine maintains separate sorted sets per (game, geographic-board, time-window, augmentation-track) tuple. This multiplies the ranking set count by 3 but is architecturally straightforward — the same insert and query logic applies to each set independently.

### 6.2 Score Screen Anatomy

When a player completes a game session and reaches the score screen, the client should present:

```
YOUR SCORE: 400  ◆ PURE
★ ALL-TIME PERSONAL BEST (Pure)   Today's best: 400   Week's best: 400   Year's best: 400

[ Pure ] [ Augmented ] [ All ]    ← track toggle; defaults to Pure
[ All Time ] [ This Year ] [ This Week ] [ Today ]   ← time window toggle; defaults to All Time

─────────────────────────────────
  WORLD                #11,076
─────────────────────────────────
  ...
  #11,074  BlueFoxTail          412
  #11,075  Jimmy Two Bellies    408
▶ #11,076  YOU                  400
  #11,077  Aunty Mildred        397
  #11,078  PigeonKing99         391
  ...

─────────────────────────────────
  COVENTRY             #35 of 980
─────────────────────────────────
  [contextual slice]

─────────────────────────────────
  BALL HILL            #2 of 2
─────────────────────────────────
  #1  DavidH                   440
▶ #2  YOU                      400

─────────────────────────────────
  LEIGHTON ROAD        #2 of 3
─────────────────────────────────
  #1  Little Johny             425
▶ #2  YOU                      400
  #3  OldDogTricks             310
```

The time window toggle switches all boards simultaneously. Switching to "Today" shows ranks computed only from scores submitted in the current day, with player counts reflecting only players who have submitted today. A narrower window will often produce smaller, more achievable competition pools — this is by design and adds daily motivation even for players who are far down the all-time board.

Display rules:
- Only boards the player is enrolled in are shown. Boards requiring narrower consent display a prompt explaining what the player would see if they opted in.
- Player count at District and Locality level is always shown alongside rank. At higher levels it is optional.
- Boards where the player has no entry in the selected time window (e.g. they have never submitted a score this week) show a "No score this window" state rather than hiding the board entirely.
- Boards where the player has no nearby competitors (large rank gap above and below) show the top 3 and the player's position, bridged with an ellipsis.

### 6.3 Live vs. Snapshot Rankings

The score screen always shows a **snapshot** rank — the rank at the moment of submission, frozen for that session's display. Live rank updates (i.e. a player's rank drifting as others play) are only surfaced in a dedicated persistent leaderboard screen within the game, not on the post-session score screen.

---

## 7. System Architecture

### 7.1 Component Overview

```
┌─────────────────────────────────────────────────┐
│                  Game Client                     │
│  (Score submission · Rank display · Consent UI) │
└───────────────┬─────────────────────────────────┘
                │ HTTPS / Game Engine SDK
                ▼
┌──────────────────────────────────────┐
│         GeoScoreboard Gateway        │
│  Auth · Rate Limiting · Routing      │
└───────┬──────────────┬───────────────┘
        │              │
        ▼              ▼
┌──────────────┐  ┌─────────────────────────┐
│ Score        │  │ Leaderboard Query        │
│ Ingestion    │  │ Service                  │
│ Service      │  │                          │
│              │  │ Serves board slices,     │
│ Validates,   │  │ personal bests, rank     │
│ resolves     │  │ lookups                  │
│ location,    │  │                          │
│ emits events │  └──────────┬──────────────┘
└──────┬───────┘             │
       │                     │
       ▼                     ▼
┌──────────────────────────────────────┐
│          Ranking Engine              │
│                                      │
│  Maintains sorted ranking sets       │
│  per board (one per geo-level per    │
│  game). Processes score events,      │
│  updates positions.                  │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│       Player Record Store            │
│                                      │
│  Player ID, Display Name,            │
│  Consent Tier, location labels,      │
│  Personal Best, board enrolments     │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│       Geolocation Service            │
│                                      │
│  Resolves location signals to        │
│  geographic labels at each tier.     │
│  Does not retain raw inputs.         │
└──────────────────────────────────────┘
```

### 7.2 Component Responsibilities

**GeoScoreboard Gateway**  
The single entry point for all game client communication. Handles authentication (game API keys + per-player session tokens), rate limiting, and routes requests to downstream services. No business logic.

**Score Ingestion Service**  
Accepts score submissions. Validates token and score plausibility. Delegates location resolution to the Geolocation Service. Reads the player's Consent Tier from the Player Record Store to determine which boards to update. Emits a score event to the Ranking Engine. Returns immediate acknowledgement to the client.

**Leaderboard Query Service**  
Serves all read requests: contextual slices, top-N lists, personal bests, rank lookups. Reads from the Ranking Engine. Stateless and horizontally scalable. Applies visibility rules — a player's Display Name is returned only if the requesting player is enrolled at an equal or narrower tier.

**Ranking Engine**  
The heart of the system. Maintains one sorted ranking set per (game, geographic-board, time-window, augmentation-track) tuple — four time windows × three tracks (Pure, Augmented, All) × N geographic boards per game. Accepts score events carrying window update flags and augmentation track, and updates only the windows and tracks where the submitted score is a new best. The `All` track is updated for every submission regardless of augmentation status. Must support: rank lookup by player, range query by rank position (for slices), and player count per board per track. O(log N) insert and rank operations are a hard requirement at the scale this system targets. Time-windowed sets (Today, This Week, This Year) are cleared on their respective reset schedules; the All-Time set never resets.

**Player Record Store**  
Source of truth for player identity, consent configuration, and personal bests. Writes on: first registration, consent tier change, personal best update. Reads on: every score submission and leaderboard query. Consistency requirement: consent tier reads must reflect the latest committed value before a score event is processed.

**Geolocation Service**  
Accepts a location signal (IP + locale for Tiers 1–3; GPS coordinates for Tiers 4–5) and returns a structured geographic label set (Country, Region, City, District, Locality). Discards all inputs after label resolution. Labels are canonical and consistent — the same physical location always resolves to the same labels. Maintains a geographic reference dataset updated no less than annually.

### 7.3 Data Flows

**Score Submission (happy path):**
1. Client sends score + session token + location signal + augmentation status (`PURE` | `AUGMENTED`) to Gateway.
2. Gateway authenticates and forwards to Score Ingestion Service.
3. Ingestion reads player's Consent Tier and existing Personal Best (per augmentation track).
4. Ingestion calls Geolocation Service; receives geographic labels.
5. Ingestion compares new score to Personal Best on the matching track; updates if higher.
6. Ingestion emits score event to Ranking Engine for each board the player is enrolled in, carrying the augmentation track.
7. Ranking Engine updates positions on the matching track's ranking set (Pure or Augmented), and on the All set regardless. Acknowledges event.
8. Ingestion returns acknowledgement to client with: previous rank on matching track (snapshot), personal best flag for that track, and a query token.
9. Client polls Leaderboard Query Service with query token; receives full score screen payload when rankings are settled.

**Consent Tier Change:**
1. Client sends new Consent Tier + appropriate location signal.
2. Player Record Store validates the request and updates the tier.
3. If tier is being reduced: Ranking Engine removes player from all boards narrower than the new tier. Player Record Store deletes location labels for those tiers.
4. If tier is being raised: Geolocation Service resolves location for new tiers. Player Record Store stores new labels. Ranking Engine enrols player in new boards using their current Personal Best as the ranking score.

---

## 8. Game Engine Integration

### 8.1 SDK Contract

The integrating game engine communicates with GeoScoreboard through an SDK that abstracts the Gateway. The SDK surface is intentionally minimal:

```
GeoScoreboard.initialise(gameId, apiKey)

GeoScoreboard.registerPlayer(displayName?) → PlayerId
  // Call once per player. displayName optional; system assigns pseudonym if absent.

GeoScoreboard.setConsentTier(playerId, tier, locationSignal?) → ConsentResult
  // Sets or changes the player's privacy tier.

GeoScoreboard.submitScore(playerId, scoreValue, sessionToken) → SubmissionReceipt
  // Returns immediately with prior rank + personal best flag.

GeoScoreboard.getScoreScreen(receipt) → ScoreScreenPayload
  // Polls/awaits the full ranked view for display. Returns all enrolled boards.

GeoScoreboard.getLeaderboard(playerId, geoLevel, viewMode, timeWindow) → LeaderboardPayload
  // Fetch a live board (contextual or top-N) for persistent leaderboard screens.
  // timeWindow: ALL_TIME | THIS_YEAR | THIS_WEEK | TODAY
```

### 8.2 Session Tokens

Session tokens are issued by the integrating game's own backend (or via the GeoScoreboard Gateway's token-issuance endpoint) and are single-use, time-bounded, and tied to a specific player-game combination. They prevent client-side score manipulation by ensuring the score submission path is authenticated end-to-end. The game is responsible for minting tokens at session start; GeoScoreboard validates them at submission.

### 8.3 Offline and Edge Cases

- If the player has no network connectivity at submission time, the client SDK caches the score locally and submits on next connection. Scores cached offline are accepted if submitted within a configurable grace window (default: 24 hours). Scores outside this window are rejected to prevent replay-based cheating.
- If location resolution fails and the player's tier requires it, the score is submitted at the highest tier that resolved successfully. The player is notified.
- If the Ranking Engine is unavailable, scores are queued durably and processed on recovery. The score screen falls back to showing the player's last known rank with a "rankings updating" indicator.

---

## 9. Board Lifecycle and Governance

### 9.1 Board Creation

Boards at World and Country level are pre-provisioned at game registration. Boards at Region, City, District, and Locality levels are created on-demand when the first player from that location is enrolled at the corresponding tier. This allows the system to grow organically with the player population without manual configuration.

### 9.2 Sparse Boards

A board exists from the moment its first player is enrolled but is **not shown to any player** until it reaches a minimum of **2 active players**. This threshold is fixed, not configurable. A single-player board has no competitive value and would expose the lone player as the only person at that location, which is a privacy concern at District and Locality level.

Once a board reaches 2 players it becomes visible to all enrolled players. It is shown without any sparse label — 2nd of 2 is a meaningful, motivating position. If active player count later drops back to 1 (e.g. due to inactivity archival), the board returns to hidden state until the threshold is met again.

### 9.3 Inactive Players

A player who has not submitted a score within a configurable inactivity window (suggested: 90 days) is moved to an archived state on all boards. Their rank position is preserved in the archive but they no longer appear in live board views. When they next submit a score, they are reinstated with their archived best as their baseline.

### 9.4 Board Reset Policy

Each board maintains four concurrent ranking sets aligned to the time windows defined in Section 5.2. Resets are automatic and UTC-anchored:

- **Today** resets at midnight UTC daily.
- **This Week** resets at Monday 00:00 UTC.
- **This Year** resets at 1 January 00:00 UTC.
- **All Time** never resets.

Resets are applied to ranking sets only. A player's stored score records for prior windows are not deleted — they form a historical log that may be used for future features (e.g. season summaries). Reset schedules are uniform across all geographic levels and are not configurable per game.

---

## 10. Out of Scope

The following are explicitly outside GeoScoreboard's boundaries and are the responsibility of the integrating game:

- Gameplay anti-cheat (detecting aimbot, automation, exploits)
- Monetisation tied to ranking (in-game rewards, prizes)
- Social features (friends lists, challenges, messaging)
- Player authentication and account management (GeoScoreboard only issues its own opaque Player IDs; the integrating game manages accounts)
- Content moderation of Display Names (the integrating game must filter before submitting to GeoScoreboard)
- Rendering of the score screen (the SDK delivers a data payload; the game renders it according to its own art direction)

---

## 11. Open Questions for Future Versions

- **Clan/group boards:** Should groups of players be able to form teams with an aggregate score? Adds significant complexity to the consent model.
- **Temporal boards at sparse local levels:** Today/This Week windows at District and Locality level may have zero or one players on any given day, causing boards to flicker in and out of the visible threshold. Consider whether the 2-player visibility rule should apply per time window independently, or only to the All Time board.
- **Cross-game boards:** Could a city board aggregate scores across multiple games on the same platform? Requires a unified scoring normalisation strategy.
- **Opt-in player search:** Should a player be able to search for a named player to find their rank, even if that player is on a board the searcher isn't enrolled in? Currently disallowed; reconsider with privacy review.
- **Historical window archive:** Prior window results (last week's best, last year's best) are retained in raw score records but not currently surfaced in any view. A season summary or personal history screen could leverage this data.
