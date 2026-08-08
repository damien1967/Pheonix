# Phoenix — Way of Working

> Companion to `CLAUDE.md` (how to work) and `PHOENIX_CORE_OVERVIEW.md` (what to build). This document covers the mechanical loop: plan → failing test → build → test → commit → distribute for test → distribute — and the build chain that makes it free forever.

---

## The Loop

Every piece of new functionality — a new `InteractionRule`, a `GameDefinition`, a UI screen — goes through the same seven stages in order. Don't skip stages; don't reorder them.

| # | Stage | Where it happens | Cost |
|---|---|---|---|
| 1 | Plan | Your head / a short note | $0 |
| 2 | Failing test | `commonTest` (or platform test tree), locally | $0 |
| 3 | Build | Locally, Android Studio / Xcode | $0 |
| 4 | Test | Locally, then confirmed by CI | $0 |
| 5 | Commit | `git push` → GitHub Actions | $0 |
| 6 | Distribute for test | GitHub Actions → testers | $0 (Android) · $99/yr (iOS, see §7) |
| 7 | Distribute (release) | Play Store / App Store | $25 once (Google) · $99/yr (Apple) |

Stages 1–5 are pure engineering and cost nothing under any circumstances. Stages 6–7 hit real platform fees — those are Apple's and Google's charges, not CI charges, and no amount of pipeline engineering removes them. Section 9 has the full ledger.

---

## 1. Plan

Before writing code:

- Name the layer you're touching: `engine`, `board`, `piece`, `mechanic`, `definition`, or `modifier` (see `CLAUDE.md` file structure).
- If the change adds mechanic behaviour, confirm it slots into one of the three hook points — step 2 (Placement Validator), step 4 (Interaction Resolver), step 6 (State Broadcast & Replenish). If it doesn't fit one of those three, stop and raise it — that's an architecture-rule violation, not a new hook to invent.
- Write down the test names you intend to write, in `given_<context>_when_<action>_then_<outcome>` form. This *is* the plan — if you can't name the tests, you don't understand the change yet.

## 2. Failing Test

- Add the test to `shared/src/commonTest/kotlin/phoenix/...`, mirroring the `commonMain` package it targets.
- Run it and confirm it fails for the *right* reason (missing behaviour, not a typo or compile error).

```
./gradlew :shared:testDebugUnitTest --tests "phoenix.mechanic.*YourNewTest*"
```

## 3. Build

Local build, before touching CI:

```
./gradlew build                      # JVM + Android, all modules
```

For an iOS framework build (needed if the change touches anything `iosMain` links against):

```
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

or simply build `iosApp.xcworkspace` in Xcode — `Product → Build`.

## 4. Test

```
./gradlew allTests                   # shared commonTest, run on the JVM target
./gradlew :composeApp:testDebugUnitTest
```

iOS-specific tests (XCTest, `iosApp/iosAppTests`) run from Xcode (`Cmd+U`) or:

```
xcodebuild test -workspace iosApp/iosApp.xcworkspace -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15'
```

You do not have to run the iOS suite locally before every commit — CI does it for you (§6). Run it locally when you're actively changing `iosMain` or SwiftUI code and want fast feedback.

## 5. Commit

- One logical change per commit. Branch per feature: `feature/<short-name>`.
- Commit message: what changed and why, not a restatement of the diff.
- Push the branch, open a PR into `main`. CI (§6) is the gate — don't merge on a red run.

---

## 6. CI — free, because the repo is public

`damien1967/Pheonix` is public, which means **GitHub Actions gives unlimited free minutes on every runner type, including macOS** — this is a real, permanent GitHub policy for public repositories, not a trial. That single fact removes the entire "Mac CI costs money" problem: there is no need to self-host a runner or keep iOS testing local-only. Let GitHub do all of it.

`.github/workflows/ci.yml` should run two jobs on every push and PR to `main`:

```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  jvm-and-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: chmod +x gradlew
      - run: ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest

  ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: chmod +x gradlew
      - run: ./gradlew :shared:iosSimulatorArm64Test
```

Both jobs are $0 forever on this repo, no minute budget to watch, no self-hosted infrastructure to babysit. Your Mac is your dev machine, not a required test executor — you can push from anywhere and trust the PR checks.

**If the repo ever needs to go private again** (e.g. you bring on paid collaborators or add real assets you don't want public), the `ios` job stops being free — macOS runners are billed at a 10x minute multiplier against the private-repo free allowance, which burns through fast. At that point, replace the `macos-latest` runner with a self-hosted runner on your own Mac (`runs-on: [self-hosted, macOS]`) — same job definition, zero Actions cost, but your Mac needs to be on and reachable to pick up jobs. Don't build that now; it's not needed while public.

**Not yet wired up:** Ktlint and Detekt are named in `CLAUDE.md` as the code-quality tools but aren't in the Gradle build yet. Add them as a third fast job (`ubuntu-latest`) when the ruleset is decided — don't let this document imply it's already enforced.

---

## 7. Distribute for Test

This is where Android and iOS genuinely diverge, because Apple requires a paid account to install on physical devices and Google doesn't.

### Android — $0, no account needed

Simplest path: build a debug/release APK in CI and attach it to a GitHub Release or a workflow artifact. Testers download and sideload.

```yaml
  distribute-android:
    needs: jvm-and-android
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: chmod +x gradlew
      - run: ./gradlew :composeApp:assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: phoenix-debug-apk
          path: composeApp/build/outputs/apk/debug/*.apk
```

Testers need "install from unknown sources" enabled once — no Play Console account, no fee.

**Optional upgrade, still free:** Firebase App Distribution gives testers an installer app, push notifications on new builds, and crash-adjacent tester management, on a genuinely free tier. Worth adding once you have more than 2–3 testers; not needed to start.

### iOS — the one unavoidable cost

To install a build on a physical iPhone, Apple requires the app be signed with a certificate tied to an **Apple Developer Program membership — $99/year**. There is no free way around this for real-device testing; it's an Apple platform tax, not a tooling choice. Once you have the membership:

- TestFlight distribution is free (no per-build or per-tester charge) and is the standard path — up to 10,000 external testers, builds expire after 90 days.
- CI can build and upload automatically (`xcodebuild archive` + `xcrun altool`/`App Store Connect API` on the `macos-latest` runner), still $0 in Actions minutes.

**If you want to defer the $99/yr:** you can build and run on the iOS Simulator indefinitely for free — useful for your own development loop, but simulator builds cannot go to real testers. Decide when you actually need outside iOS testers; don't pay before then.

---

## 8. Distribute (Release)

| Store | One-time / recurring cost | Notes |
|---|---|---|
| Google Play | $25 once | Registers your Play Console account for life; no further store fee. |
| Apple App Store | $99/year | Same account as TestFlight (§7) — no separate release-only fee. |

The pipeline is the same as §7 with a tagged release trigger (`on: push: tags: ['v*']`) instead of every push to `main`, and uploads to the store's production/internal track instead of a GitHub Release. Build this out when you're actually ready to ship, not before — it's config on top of the same jobs, not new infrastructure.

---

## 9. Cost Ledger

| Item | Cost | Recurs? |
|---|---|---|
| GitHub Actions, ubuntu-latest | $0 | Forever (public repo) |
| GitHub Actions, macos-latest | $0 | Forever (public repo) |
| GitHub Releases (Android APK hosting) | $0 | Forever |
| Firebase App Distribution (optional) | $0 | Forever, free tier |
| Apple Developer Program | $99 | Yearly — only once you need real-device iOS testers or App Store release |
| Google Play Console | $25 | Once, ever |

Everything under your own control — the entire build/test/commit/CI loop, and Android distribution — is free with no asterisk, for as long as this repo stays public. The only real money in this whole pipeline is Apple's membership fee, and it buys you both TestFlight and the App Store, so it's paid once per year, not once per stage.

---

## 10. Definition of Done

A change is done when:

- [ ] Test(s) were written before the implementation and named `given_when_then`
- [ ] `./gradlew build` and `./gradlew allTests` pass locally
- [ ] No architecture rule from `CLAUDE.md` was bent to make it work — or it was raised and agreed first
- [ ] PR is open against `main`, both CI jobs (`jvm-and-android`, `ios`) are green
- [ ] Commit messages explain why, not just what
