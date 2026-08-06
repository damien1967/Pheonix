#!/bin/bash

# =============================================================================
# macOS Developer Environment Setup Script
# Installs: Homebrew, Xcode check, JDK 17, Git, CocoaPods, kdoctor,
#           Node.js (via nvm), Claude Code — then preps the Phoenix iOS
#           project (podspec + dummy framework + pod install) if this script
#           is sitting inside the project folder.
#
# HOW TO RUN THIS SCRIPT:
#   1. Open Terminal
#   2. Run: chmod +x setup_dev_env.sh
#   3. Run: ./setup_dev_env.sh
#
# It's safe to run more than once — every step checks what's already done
# and skips it, so a partial or failed run can just be re-run.
# =============================================================================

set -e  # Stop the script immediately if any command fails

# --- Helper functions --------------------------------------------------------

print_step() {
    echo ""
    echo "============================================"
    echo "  $1"
    echo "============================================"
}

print_ok() {
    echo "✅  $1"
}

print_info() {
    echo "ℹ️   $1"
}

print_warn() {
    echo ""
    echo "⚠️   $1"
}

verify() {
    # Usage: verify "git --version" "git"
    # Runs a command and checks it succeeds. Exits with a message if not.
    local cmd="$1"
    local name="$2"
    if eval "$cmd" &>/dev/null; then
        print_ok "$name is installed: $(eval $cmd 2>&1)"
    else
        echo ""
        echo "❌  ERROR: '$name' doesn't seem to be installed correctly."
        echo "    Try closing and reopening Terminal, then run this script again."
        echo "    If the problem persists, paste the error above and ask for help."
        exit 1
    fi
}

# --- Step 1: Homebrew --------------------------------------------------------
# Homebrew is a package manager for macOS — think of it as an App Store
# for developer tools you install from the Terminal.

print_step "STEP 1 of 9: Installing Homebrew"

if command -v brew &>/dev/null; then
    print_info "Homebrew is already installed. Skipping."
else
    print_info "Installing Homebrew. You may be prompted for your Mac password."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

verify "brew --version" "Homebrew"

# Homebrew directories can end up owned by a different macOS user account than
# the one you're running as now (e.g. after switching to a new user account on
# the same Mac). When that happens, every 'brew install'/'brew upgrade' fails
# with a confusing "Permission denied" error. Detect it up front and fix it
# once here, rather than hitting it partway through a later step.
print_info "Checking Homebrew directory permissions..."

brew_prefix="$(brew --prefix)"
needs_perm_fix=false
for dir in "$brew_prefix/bin" "$brew_prefix/etc" "$brew_prefix/lib" "$brew_prefix/share" "$brew_prefix/Cellar" "$brew_prefix/var"; do
    if [ -d "$dir" ] && [ ! -w "$dir" ]; then
        needs_perm_fix=true
        break
    fi
done

if [ "$needs_perm_fix" = true ]; then
    print_info "Some Homebrew directories aren't owned by $(whoami) — fixing (needs your Mac password)..."
    sudo chown -R "$(whoami)":admin "$brew_prefix"
    print_ok "Homebrew permissions fixed."
else
    print_ok "Homebrew permissions already OK."
fi

# --- Step 2: Xcode ------------------------------------------------------------
# Xcode itself can only be installed from the App Store (no CLI install), so
# this step checks and gives instructions rather than installing anything.
# It does NOT stop the script — later steps don't depend on Xcode being ready.

print_step "STEP 2 of 9: Checking Xcode"

if xcodebuild -version &>/dev/null; then
    print_ok "Xcode is installed: $(xcodebuild -version | head -1)"
else
    print_warn "Xcode isn't installed or hasn't been opened yet."
    echo "    This script can't install it — it must come from the App Store:"
    echo "      1. Open the App Store app"
    echo "      2. Search for 'Xcode' and click Get/Install (~15 GB, takes a while)"
    echo "      3. Open Xcode once, accept the licence, let it finish installing components"
    echo "      4. Re-run this script afterwards"
    print_info "Continuing with the rest of the setup..."
fi

# Command Line Tools are separate from the full Xcode app and are needed by
# Gradle's iOS build tasks even before the full Xcode app has finished setup.
if ! xcode-select -p &>/dev/null; then
    print_info "Installing Xcode Command Line Tools (a popup will appear)..."
    xcode-select --install || true
    print_info "Click 'Install' in the popup, then re-run this script once it finishes."
else
    print_ok "Command Line Tools are installed."
fi

# --- Step 3: JDK 17 -------------------------------------------------------
# Gradle and the Kotlin Multiplatform toolchain require JDK 17 specifically.

print_step "STEP 3 of 9: Installing JDK 17"

if /usr/libexec/java_home -v 17 &>/dev/null; then
    print_info "JDK 17 is already installed. Skipping."
else
    print_info "Installing JDK 17 (Temurin build) via Homebrew..."
    brew install --cask temurin@17
fi

if ! /usr/libexec/java_home -v 17 &>/dev/null; then
    echo ""
    echo "❌  ERROR: JDK 17 still isn't showing up after installing."
    echo "    Try closing and reopening Terminal, then run this script again."
    exit 1
fi

jdk17_home="$(/usr/libexec/java_home -v 17)"
print_ok "JDK 17 found at: $jdk17_home"

# The system default 'java' can still point at an older JDK even after this.
# Gradle needs JAVA_HOME pointed at 17 explicitly, so pin it in .zshrc.
if [ ! -f "$HOME/.zshrc" ]; then
    touch "$HOME/.zshrc"
fi

if ! grep -q "^export JAVA_HOME=" "$HOME/.zshrc" 2>/dev/null; then
    print_info "Pointing JAVA_HOME at JDK 17 in .zshrc..."
    cat >> "$HOME/.zshrc" << EOF

# JAVA_HOME (JDK 17, for Gradle / Kotlin Multiplatform)
export JAVA_HOME="$jdk17_home"
EOF
fi

# --- Step 4: Git -------------------------------------------------------------
# Git is version control software. It tracks every change you make to your
# code, lets you undo mistakes, and is used by almost every developer on earth.

print_step "STEP 4 of 9: Installing Git"

# If an old version of gettext is blocking the install, unlink it first
if brew list gettext &>/dev/null; then
    print_info "Found existing gettext install — unlinking to avoid conflicts..."
    brew unlink gettext 2>/dev/null || true
fi

brew install git

verify "git --version" "Git"

# Set up your identity — Git stamps every saved change with your name and email
if [ -z "$(git config --global user.name 2>/dev/null)" ]; then
    print_info "Configuring Git identity..."
    read -p "  Enter your name (e.g. Jane Smith): " git_name
    read -p "  Enter your email (e.g. jane@example.com): " git_email

    git config --global user.name "$git_name"
    git config --global user.email "$git_email"

    print_ok "Git configured for: $git_name <$git_email>"
else
    print_info "Git identity already configured — skipping."
fi

# --- Step 5: CocoaPods ---------------------------------------------------
# CocoaPods links the shared Kotlin framework into the iOS Xcode project.
# Xcode 16 changed its project file format — it can now use a "synchronized
# folder" reference (PBXFileSystemSynchronizedRootGroup) instead of listing
# every file individually. CocoaPods versions older than ~1.15 don't
# understand that format and fail with an Xcodeproj parse error. Installing
# via Homebrew (rather than 'sudo gem install cocoapods') keeps this current
# with a single 'brew upgrade' and avoids system-Ruby gem conflicts.

print_step "STEP 5 of 9: Installing CocoaPods"

if brew list --formula cocoapods &>/dev/null; then
    print_info "CocoaPods already installed via Homebrew — checking for updates..."
    brew upgrade cocoapods 2>/dev/null || print_info "Already the latest version."
else
    print_info "Installing CocoaPods via Homebrew..."
    brew install cocoapods
fi

verify "pod --version" "CocoaPods"

pod_version="$(pod --version 2>/dev/null | tail -1)"
pod_major="$(echo "$pod_version" | cut -d. -f1)"
pod_minor="$(echo "$pod_version" | cut -d. -f2)"
if [ "$pod_major" -lt 1 ] || { [ "$pod_major" -eq 1 ] && [ "$pod_minor" -lt 15 ]; }; then
    print_warn "CocoaPods $pod_version is older than 1.15."
    echo "    'pod install' may fail on Xcode 16 projects with a"
    echo "    PBXFileSystemSynchronizedRootGroup error. Run 'brew upgrade cocoapods'"
    echo "    manually if you hit that."
fi

# --- Step 6: kdoctor -------------------------------------------------------
# kdoctor inspects the whole KMP toolchain (Xcode, JDK, CocoaPods, Android)
# in one pass. It's informational only — this step never stops the script,
# since some things it checks (like Android Studio) need a manual GUI install.

print_step "STEP 6 of 9: Running kdoctor (KMP environment health check)"

if ! command -v kdoctor &>/dev/null; then
    print_info "Installing kdoctor..."
    brew install kdoctor
fi

print_info "[!] warnings are usually fine; [x] errors need fixing before Android/iOS builds will work."
kdoctor || true

# --- Step 7: Node.js (via nvm) -----------------------------------------------
# Node.js lets you run JavaScript outside a web browser — on your own computer.
# It also installs 'npm', the tool you use to install JavaScript packages/libraries.
#
# We install Node via 'nvm' (Node Version Manager) rather than directly,
# because nvm lets you easily switch between Node versions for different projects.

print_step "STEP 7 of 9: Installing Node.js via nvm"

if [ -d "$HOME/.nvm" ]; then
    print_info "nvm is already installed. Skipping nvm install."
else
    print_info "Downloading and installing nvm..."
    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
fi

# Add nvm initialisation to .zshrc if it's not already there
if ! grep -q 'NVM_DIR' "$HOME/.zshrc"; then
    print_info "Adding nvm to .zshrc..."
    cat >> "$HOME/.zshrc" << 'EOF'

# nvm (Node Version Manager)
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
EOF
fi

# Load nvm into the current terminal session so we can use it right now
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

print_info "Installing the latest LTS (long-term support) version of Node..."
nvm install --lts
nvm use --lts

verify "node --version" "Node.js"
verify "npm --version" "npm"

# --- Step 8: Claude Code -----------------------------------------------------
# Claude Code is Anthropic's command-line tool that lets you delegate coding
# tasks to Claude directly from your Terminal.

print_step "STEP 8 of 9: Installing Claude Code"

npm install -g @anthropic-ai/claude-code

verify "claude --version" "Claude Code"

# --- Step 9: Phoenix KMP project bootstrap (conditional) --------------------
# CocoaPods needs two things to exist before 'pod install' can succeed:
#   1. shared/shared.podspec    — generated by Gradle's CocoaPods plugin
#   2. a dummy shared.framework — a placeholder so Xcode has something to
#                                 link against the very first time
# Neither exists until Gradle has evaluated the project at least once. This
# step only runs if the script is actually sitting inside the Phoenix project.

print_step "STEP 9 of 9: Preparing the Phoenix KMP project"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -f "$script_dir/settings.gradle.kts" ] && [ -f "$script_dir/iosApp/Podfile" ]; then
    print_info "Phoenix project detected at $script_dir"
    project_ready=true

    print_info "Generating the CocoaPods podspec (may take a minute on first run)..."
    if ! JAVA_HOME="$jdk17_home" "$script_dir/gradlew" -p "$script_dir" :shared:podspec; then
        print_info "Podspec generation failed — skipping the rest of the iOS project prep."
        print_info "Fix the Gradle error above, then re-run this script."
        project_ready=false
    fi

    if [ "$project_ready" = true ]; then
        print_info "Generating the dummy shared framework..."
        if ! JAVA_HOME="$jdk17_home" "$script_dir/gradlew" -p "$script_dir" :shared:generateDummyFramework; then
            print_info "Dummy framework generation failed — skipping 'pod install'."
            project_ready=false
        else
            print_info "Note: the dummy framework only unblocks 'pod install'. Building"
            print_info "the app in Xcode (Cmd+R) still needs at least one real Kotlin"
            print_info "declaration in shared/src/commonMain — an empty commonMain makes"
            print_info "Kotlin/Native skip framework generation entirely, and Xcode's"
            print_info "build fails with 'FrameworkCopy ... doesn't exist'."
        fi
    fi

    if [ "$project_ready" = true ]; then
        print_info "Running 'pod install' in iosApp/..."
        if (cd "$script_dir/iosApp" && pod install); then
            print_ok "Phoenix iOS project is ready — open iosApp/iosApp.xcworkspace in Xcode."

            if [ -d "$script_dir/iosApp/.git" ]; then
                print_warn "Found a nested .git inside iosApp/ — this is Xcode's"
                echo "    'Create Git repository on my Mac' checkbox from when the"
                echo "    .xcodeproj was created. It will conflict with git at the"
                echo "    Phoenix project root (Stage 11). Remove it with:"
                echo "      rm -rf \"$script_dir/iosApp/.git\""
            fi
        else
            print_warn "'pod install' didn't complete."
            echo "    This is expected if you haven't created the Xcode project yet —"
            echo "    iosApp/*.xcodeproj only exists once you create it in Xcode."
            echo "    See iosApp/README.md for the two steps needed first, then run"
            echo "    'cd iosApp && pod install' yourself."
        fi
    fi
else
    print_info "Not inside the Phoenix project yet — skipping project-specific setup."
    print_info "Once the project exists, re-run this script from its root directory"
    print_info "to auto-generate the podspec, dummy framework, and run 'pod install'."
fi

# --- Done! -------------------------------------------------------------------

echo ""
echo "============================================"
echo "  ALL DONE! Here's a summary:"
echo "============================================"
echo ""
echo "  Homebrew:    $(brew --version | head -1)"
echo "  Xcode:       $(xcodebuild -version 2>/dev/null | head -1 || echo 'not installed — see Step 2 above')"
echo "  JDK 17:      $(/usr/libexec/java_home -v 17 2>/dev/null || echo 'not found')"
echo "  Git:         $(git --version)"
echo "  CocoaPods:   $(pod --version 2>/dev/null | tail -1)"
echo "  Node.js:     $(node --version)"
echo "  npm:         $(npm --version)"
echo "  Claude Code: $(claude --version)"
echo ""
echo "  Next step: run 'claude' to log in to your Anthropic account."
echo ""
echo "  ⚠️  IMPORTANT: Close and reopen Terminal before using these tools."
echo "     This ensures all the PATH changes take effect properly."
echo ""
