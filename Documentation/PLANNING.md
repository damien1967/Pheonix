# Phoenix — Planning

Tickets and timeline live on GitHub, not as files in this repo. This document is the reference for how they're organised and the exact `gh` commands to work with them from the command line.

- **Tickets:** GitHub Issues on `damien1967/Pheonix`
- **Timeline / grid:** [Phoenix Roadmap](https://github.com/users/damien1967/projects/1) — a GitHub Project with a Table view (spreadsheet grid) and a Roadmap view (Gantt-style timeline)

---

## Fields

| Field | Values | Meaning |
|---|---|---|
| `Status` | Todo / In Progress / Done | Built-in Project field |
| `Priority` | P0 / P1 / P2 | P0 = blocking current phase, P2 = deferred/nice-to-have |
| `Start date` | date | When work is expected to begin |
| `Target date` | date | When work is expected to land |

Dates are easiest to set by dragging bars in the Roadmap view in a browser — the `gh project` CLI can set them too but needs field IDs looked up first, which is more friction than it's worth for a solo project.

## Labels

Layer labels mirror `CLAUDE.md`'s file structure: `engine`, `board`, `piece`, `mechanic`, `definition`, `modifier`, `shell`. Platform labels: `android`, `ios`. Plus `blocked` — see Dependencies below.

---

## Dependencies

GitHub doesn't have a first-class "blocked by" field usable from the CLI, so this project uses a plain convention instead:

- A blocked ticket gets the `blocked` label
- Its issue body includes a line: `Blocked by: #N`
- Unblocking means removing the label once #N closes

This is a manual convention — nothing enforces it automatically. Keep it accurate by hand.

---

## Command Reference

**What's next** (open, unblocked, sorted by priority — check the `Priority` column visually in Table view, or):
```
gh issue list --repo damien1967/Pheonix --state open --search "-label:blocked"
```

**What's blocked, and on what:**
```
gh issue list --repo damien1967/Pheonix --label blocked
gh issue view <number> --repo damien1967/Pheonix   # body shows "Blocked by: #N"
```

**Create a ticket:**
```
gh issue create --repo damien1967/Pheonix \
  --title "..." \
  --body "..." \
  --label engine
```

**Add an existing issue to the Roadmap project:**
```
gh project item-add 1 --owner damien1967 --url <issue-url>
```

**See everything on the board:**
```
gh project item-list 1 --owner damien1967
```

**Close a ticket** (or just reference `Closes #N` in the commit that resolves it, per `WAY_OF_WORKING.md`):
```
gh issue close <number> --repo damien1967/Pheonix
```

---

## Relationship to Other Documents

`high level implementation plan.md` defines the phases and what each one delivers. Tickets are the phases broken into individually workable pieces — one rule-set or one structural entity per ticket, generally. `WAY_OF_WORKING.md` covers the engineering loop each ticket goes through (plan → failing test → build → test → commit).
