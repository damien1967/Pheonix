# Phoenix — Mac Environment Setup Guide

> For: macOS (Apple Silicon or Intel) · KMP + Android Studio + Xcode + Claude Code
> Complete this guide in order — each stage depends on the one before it.

---

## Before You Start

**Check your chip.** Click the Apple menu → **About This Mac**. Under the chip or processor line you will see either **Apple M1/M2/M3/M4** (Apple Silicon) or **Intel Core i_**. Note which one you have — a few download steps differ.

**Allow enough time.** Xcode alone is ~15 GB. Stage 2 will take a while on any connection. Start it first, then read ahead while it downloads.

**Terminal.** Most steps use Terminal. Find it at `Applications → Utilities → Terminal`, or press `Cmd+Space` and type `terminal`. Every command in this guide is run in Terminal unless stated otherwise.

> **Shortcut:** `setup_dev_env.sh` in this repo automates Stages 3 (Homebrew, including the permissions fix), 4 (JDK 17), 6 (CocoaPods), 7 (kdoctor), and — once the project exists — the CocoaPods prep at the end of Stage 10. Run `./setup_dev_env.sh` and skip straight to whichever manual stage it doesn't cover (2, 5, 8, 9, 11+). It's safe to run more than once. The stages below stay as the manual/explained version in case the script hits something it doesn't handle.

---

## Stage 1 — Project Folder & Docs

1. [ ] Create a project folder — somewhere tidy, e.g. in your home directory:
    ```
    mkdir -p ~/Dev/phoenix
    ```
2. [ ] Copy `CLAUDE.md` into that folder
3. [ ] Copy `PHOENIX_CORE_OVERVIEW.md` into that folder
4. [ ] Any other `.md` spec files go in the root of that folder too — Claude Code reads them automatically on startup

---

## Stage 2 — Xcode

Xcode must be installed before Android Studio. The KMP toolchain needs Xcode's command-line tools to build the iOS framework.

5. [ ] Open the **App Store** app on your Mac (not a browser — Xcode must come from the App Store)
6. [ ] Search for **Xcode** and click **Get** / **Install** — it is large (~15 GB), this will take time. Let it run in the background and continue reading.
7. [ ] Once installed, **open Xcode once** — it will ask you to install additional components and accept a licence agreement. Click **Install** and **Agree**. Wait for it to finish before closing.
8. [ ] Back in Terminal, run:
    ```
    xcode-select --install
    ```
    A popup will appear asking to install Command Line Developer Tools — click **Install**. If it says "already installed", that is fine, move on.
9. [ ] Verify Xcode is wired up correctly:
    ```
    xcodebuild -version
    ```
    You should see something like `Xcode 16.x` — not an error. If you see an error, Xcode did not finish installing properly — try opening Xcode again and waiting for it to complete its setup.

---

## Stage 3 — Homebrew

Homebrew is the standard package manager for Mac. You will use it to install several tools in later stages.

10. [ ] Check if Homebrew is already installed:
    ```
    brew --version
    ```
    If you see a version number, skip to Stage 4. If you see "command not found", install it now:
11. [ ] Install Homebrew by pasting this entire line into Terminal and pressing Enter:
    ```
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    ```
    Enter your Mac password when prompted. It will take a few minutes.

    **Apple Silicon only — one extra step:** After the installer finishes it will print two commands under "Next steps" — they look like this:
    ```
    echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
    eval "$(/opt/homebrew/bin/brew shellenv)"
    ```
    Run both of those commands. Intel Macs do not need this.

12. [ ] Verify:
    ```
    brew --version
    ```
    You should see `Homebrew x.x.x`.

> **If `brew install` fails with "Permission denied":** Homebrew's directories under `/usr/local` can end up owned by a different macOS user account than the one you're currently using — this happens after switching to a new account on the same Mac. Fix it with:
> ```
> sudo chown -R $(whoami):admin /usr/local
> ```
> Enter your Mac password when prompted, then retry the install.

---

## Stage 4 — Java (JDK 17)

Gradle and Kotlin require a Java Development Kit. Use version 17 — it is the long-term support version that Android Studio expects.

13. [ ] Install JDK 17 via Homebrew:
    ```
    brew install --cask temurin@17
    ```
14. [ ] Open a **new Terminal window** (important — the old one won't see the new installation) and verify:
    ```
    java -version
    ```
    You should see `openjdk version "17.x.x"`. If you see a different version, your Mac has a pre-existing Java install taking priority — this is fine for Android Studio, which uses its own bundled JDK regardless.

> **For command-line Gradle (`./gradlew ...`) specifically:** Android Studio's bundled JDK doesn't help outside the IDE — `gradlew` uses whatever `JAVA_HOME` resolves to. Point it at 17 explicitly:
> ```
> echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
> source ~/.zshrc
> ```

---

## Stage 5 — Android Studio

15. [ ] Go to https://developer.android.com/studio in your browser
16. [ ] Click **Download Android Studio** — make sure you pick the right version for your chip:
    - Apple Silicon → download the **Mac (Apple Silicon)** `.dmg`
    - Intel → download the **Mac (Intel)** `.dmg`
17. [ ] Open the `.dmg` file, drag **Android Studio** into your **Applications** folder, then eject the disk image
18. [ ] Open Android Studio from Applications. macOS may warn you it was downloaded from the internet — click **Open** if asked.
19. [ ] The **Setup Wizard** will launch. When asked, choose **Standard** installation and click through. This downloads the Android SDK, build tools, and an emulator system image. It will take several minutes. Let it finish completely before moving on.
20. [ ] You will land on the **Welcome to Android Studio** screen. Do not open a project yet.

**Install the Kotlin Multiplatform plugin:**

21. [ ] On the Welcome screen, click **Plugins** in the left sidebar
22. [ ] In the search box type **Kotlin Multiplatform**
23. [ ] Click **Install** on the result from JetBrains
24. [ ] When prompted, click **Restart IDE** and let Android Studio reopen

**Note the Android SDK location:**

25. [ ] Go to **Android Studio → Settings** (or press `Cmd+,`) → **Languages & Frameworks → Android SDK**
26. [ ] Copy the path shown in **Android SDK Location** — it will usually be `/Users/YOUR_NAME/Library/Android/sdk`

**Set ANDROID_HOME in your shell:**

27. [ ] In Terminal, open your shell config file:
    ```
    nano ~/.zshrc
    ```
    (If you customised your shell and use bash, use `~/.bash_profile` instead — but on modern Macs the default is zsh.)
28. [ ] Use the arrow keys to go to the very bottom of the file. Add these two lines, replacing the path with the one you copied in step 26 if it was different:
    ```
    export ANDROID_HOME=$HOME/Library/Android/sdk
    export PATH=$PATH:$ANDROID_HOME/platform-tools
    ```
29. [ ] Save and exit: press `Ctrl+X`, then `Y`, then `Enter`
30. [ ] Reload your shell and verify:
    ```
    source ~/.zshrc
    echo $ANDROID_HOME
    ```
    It should print the SDK path, not a blank line.

---

## Stage 6 — CocoaPods

CocoaPods is used by KMP to link the shared Kotlin framework into the Xcode project.

Install it via Homebrew, not `sudo gem install cocoapods` — the gem-based install and a Homebrew-based install can both end up on your machine at once, and whichever `pod` binary is first on your `PATH` wins, which gets confusing fast. Homebrew also makes it a one-command upgrade later, which matters (see the version note below).

31. [ ] Install CocoaPods:
    ```
    brew install cocoapods
    ```
    If it's already installed and outdated, upgrade instead:
    ```
    brew upgrade cocoapods
    ```
32. [ ] Verify:
    ```
    pod --version
    ```
    You should see a version number like `1.x.x`.

    > **Xcode 16 requires CocoaPods ≥ 1.15.** Xcode 16 can save new projects using a "synchronized folder" reference (`PBXFileSystemSynchronizedRootGroup`) instead of listing every file individually. Older CocoaPods versions can't parse that and `pod install` fails with an Xcodeproj error mentioning that ISA. If you hit that, `brew upgrade cocoapods` and retry.

---

## Stage 7 — kdoctor (Environment Health Check)

`kdoctor` is JetBrains' tool that inspects your entire KMP environment and tells you exactly what is missing or broken. Run it now, before creating a project.

33. [ ] Install kdoctor:
    ```
    brew install kdoctor
    ```
34. [ ] Run it:
    ```
    kdoctor
    ```
35. [ ] Read the output carefully:
    - **[v]** lines are fine
    - **[!]** lines are warnings — usually fine, read the message
    - **[x]** lines are errors — these must be fixed before continuing

36. [ ] Fix any errors. The most common ones on a fresh Mac are:
    - "Xcode is not configured" → open Xcode and accept the licence (Stage 2, step 7)
    - "ANDROID_HOME not set" → Stage 5, steps 27–30
    - "CocoaPods not found" → Stage 6
    - "JDK not found" → Stage 4

37. [ ] Re-run `kdoctor` after each fix. Continue only when there are no `[x]` errors.

---

## Stage 8 — Create the KMP Project

38. [ ] Open https://kmp.jetbrains.com in your browser
39. [ ] Fill in the form:
    - **Project Name:** `Phoenix`
    - **Project ID:** something like `dev.phoenix.core` (reverse-domain style, no spaces)
    - Under **Android** — tick the checkbox, select **Jetpack Compose**
    - Under **iOS** — tick the checkbox, then for **Share UI** select **No** (we use SwiftUI natively, not Compose on iOS)
    - Leave **Web** and **Desktop** unchecked for now
40. [ ] Click **Download** — you will get a `.zip` file
41. [ ] Unzip it. You should get a folder containing `composeApp/`, `iosApp/`, `shared/`, and some Gradle files.
42. [ ] Move the **contents** of that folder (not the folder itself) into your project folder from Stage 1. Your project folder should now look like:
    ```
    ~/Dev/phoenix/
    ├── CLAUDE.md
    ├── PHOENIX_CORE_OVERVIEW.md
    ├── composeApp/
    ├── iosApp/
    ├── shared/
    ├── build.gradle.kts
    └── settings.gradle.kts
    ```

---

## Stage 9 — First Android Run

43. [ ] In Android Studio click **Open** and select your project folder (`~/Dev/phoenix`)
44. [ ] Gradle will start syncing automatically — watch the progress bar at the bottom of the screen. The first sync downloads dependencies and can take 5–10 minutes. Wait for it to finish with no red errors in the **Build** panel. Yellow warnings are fine.
45. [ ] Open **Device Manager** — look for it in the right-side toolbar, or go to **View → Tool Windows → Device Manager**
46. [ ] Click **+** → **Create Virtual Device**
47. [ ] Select any **Pixel** model (Pixel 8 is a good default), click **Next**
48. [ ] Select the latest stable **API level** (the one that does not say "Recommended" in grey, but has a download arrow if needed — download it). Click **Next**, then **Finish**.
49. [ ] Back in the Device Manager, click the **Play** button next to your new emulator to start it. Wait for the emulator to fully boot to the Android home screen.
50. [ ] In the main toolbar, make sure **composeApp** is selected in the run configuration dropdown, then click the green **Run** button (or press `Ctrl+R`)
51. [ ] The app should build and appear on the emulator showing "Hello World" or similar. If it does, Stage 9 is complete.

---

## Stage 10 — First iOS Run

52. [ ] Before the first `pod install`, CocoaPods needs two things that Gradle generates, not Xcode — a podspec and a placeholder framework. From the project root:
    ```
    ./gradlew :shared:podspec
    ./gradlew :shared:generateDummyFramework
    ```
    Skip this if `./setup_dev_env.sh` already ran — its last step does this automatically.
53. [ ] `pod install` also needs an actual `.xcodeproj` to attach to — if you don't have one yet (e.g. this project's `iosApp/` only has `iosApp.swift`/`ContentView.swift` staged, no `.xcodeproj`), create it first: Xcode → **File → New → Project → iOS → App**, name it `iosApp`, save it into `iosApp/`. See `iosApp/README.md` for the exact steps.
54. [ ] In Terminal, navigate into the `iosApp` folder:
    ```
    cd ~/Dev/phoenix/iosApp
    ```
55. [ ] Run CocoaPods to link the shared framework:
    ```
    pod install
    ```
    This generates an `iosApp.xcworkspace` file.

    > **"Could not automatically select an Xcode project"** means step 53 above is missing — CocoaPods can't proceed without a `.xcodeproj` already in `iosApp/`.
    > **"No podspec found for `shared`"** means step 52 above hasn't been run yet.
56. [ ] Open the workspace in Xcode — **always open the `.xcworkspace`, not the `.xcodeproj`**:
    ```
    open iosApp.xcworkspace
    ```
57. [ ] In Xcode, click the device selector at the top of the window (it will say something like "Any iOS Device"). Choose an iPhone simulator — **iPhone 16** or similar.
58. [ ] Press `Cmd+R` to build and run
59. [ ] The iOS Simulator will open and the app should appear. If you see "Hello World" or similar, Stage 10 is complete.

> **If you get a build error about signing:** Go to Xcode → click on the `iosApp` project in the left panel → select the `iosApp` target → **Signing & Capabilities** tab → tick **Automatically manage signing** → select your Apple ID from the Team dropdown (add your Apple ID under Xcode → Settings → Accounts if needed). Free Apple IDs work for simulator builds.

---

## Stage 11 — GitHub

60. [ ] Go to https://github.com and sign in or create a free account
61. [ ] Click **New** (or the **+** menu → **New repository**)
62. [ ] Set:
    - **Repository name:** `phoenix`
    - **Visibility:** Private
    - **Do not** tick "Add a README file" (your project already has files — adding one here causes a conflict)
63. [ ] Click **Create repository**. GitHub will show you a page with setup instructions — leave it open.

**Add a .gitignore first:**

64. [ ] Go to https://www.toptal.com/developers/gitignore
65. [ ] In the search box type and select: `Android`, `Kotlin`, `Xcode`, `Gradle`, `macOS` — add all five
66. [ ] Click **Create** and copy all the generated text
67. [ ] In Terminal:
    ```
    nano ~/Dev/phoenix/.gitignore
    ```
    Paste the copied text, save and exit (`Ctrl+X`, `Y`, `Enter`)

**Push your project to GitHub:**

68. [ ] In Terminal, run these commands one at a time (replace `YOUR_USERNAME` with your GitHub username):
    ```
    cd ~/Dev/phoenix
    git init
    git add .
    git commit -m "Initial KMP scaffold"
    git remote add origin https://github.com/YOUR_USERNAME/phoenix.git
    git branch -M main
    git push -u origin main
    ```
    If prompted for a username and password: GitHub no longer accepts passwords — you need a **Personal Access Token**. Go to GitHub → Settings → Developer Settings → Personal access tokens → Tokens (classic) → Generate new token. Give it `repo` scope, copy the token, and use it as the password.

69. [ ] Refresh your GitHub repository page in the browser — you should see all your project files listed.

---

## Stage 12 — GitHub Actions CI

This runs your tests automatically every time you push code. It catches broken changes before they become a problem.

70. [ ] In Terminal:
    ```
    mkdir -p ~/Dev/phoenix/.github/workflows
    ```
71. [ ] Create the CI workflow file:
    ```
    nano ~/Dev/phoenix/.github/workflows/ci.yml
    ```
72. [ ] Paste in the following, then save and exit:
    ```yaml
    name: CI

    on:
      push:
        branches: [ main ]
      pull_request:
        branches: [ main ]

    jobs:
      test:
        runs-on: ubuntu-latest
        steps:
          - uses: actions/checkout@v4
          - uses: actions/setup-java@v4
            with:
              java-version: '17'
              distribution: 'temurin'
          - name: Grant execute permission for gradlew
            run: chmod +x gradlew
          - name: Run shared tests
            run: ./gradlew :shared:testDebugUnitTest
    ```
73. [ ] Commit and push:
    ```
    cd ~/Dev/phoenix
    git add .
    git commit -m "Add CI workflow"
    git push
    ```
74. [ ] On GitHub, click the **Actions** tab — you should see a workflow run appear within a few seconds. A green tick means tests passed. A red cross means look at the logs — at this stage it is most likely a Gradle configuration issue.

---

## Stage 13 — Claude Code

75. [ ] Check if Node.js is installed:
    ```
    node --version
    ```
    If you see a version number, skip to step 77. If you see "command not found":
76. [ ] Install Node.js via Homebrew:
    ```
    brew install node
    ```
77. [ ] Install Claude Code:
    ```
    npm install -g @anthropic-ai/claude-code
    ```
78. [ ] Navigate into your project folder and start a session:
    ```
    cd ~/Dev/phoenix
    claude
    ```
79. [ ] Claude Code will read `CLAUDE.md` and `PHOENIX_CORE_OVERVIEW.md` automatically. Verify it has context by asking:
    ```
    What is a GameDefinition in this project?
    ```
    It should answer using the Phoenix engine terminology without you having to explain anything. If it does, you are set up.

---

## You Are Done

Your Mac now has a fully working KMP development environment:

| What | Status |
|---|---|
| Xcode + Command Line Tools | Installed |
| JDK 17 | Installed |
| Android Studio + KMP plugin | Installed |
| CocoaPods | Installed |
| KMP project | Running on Android emulator and iOS simulator |
| GitHub | Project pushed, private repo |
| CI | Tests run automatically on every push |
| Claude Code | Reading your project spec on every session |

**Next:** Replace the KMP scaffold's placeholder code with the Phoenix engine structure described in the File Structure Conventions section of `CLAUDE.md`.
