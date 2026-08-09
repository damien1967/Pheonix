#!/bin/bash
# Runs the local test suite. Continues through all requested stages even if
# one fails, so you see everything that's broken in one pass rather than
# stopping at the first failure.
#
# Usage:
#   ./run_tests.sh                # everything (All Tests + Kotlin + iOS + Android)
#   ./run_tests.sh kt              # shared Kotlin tests only
#   ./run_tests.sh ios             # iOS simulator tests only
#   ./run_tests.sh android         # composeApp Android tests only
#   ./run_tests.sh kt ios          # any combination

cd "$(dirname "$0")" || exit 1

RUN_ALL=false
RUN_KT=false
RUN_IOS=false
RUN_ANDROID=false

if [ "$#" -eq 0 ]; then
    RUN_ALL=true
    RUN_KT=true
    RUN_IOS=true
    RUN_ANDROID=true
else
    for arg in "$@"; do
        case "$arg" in
            kt)      RUN_KT=true ;;
            ios)     RUN_IOS=true ;;
            android) RUN_ANDROID=true ;;
            *)
                echo "Unknown parameter: $arg"
                echo "Usage: $0 [kt] [ios] [android]"
                echo "  (no parameters runs everything)"
                exit 1
                ;;
        esac
    done
fi

STAGE_NAMES=()
STAGE_RESULTS=()

run_stage() {
    local name="$1"
    shift
    echo ""
    echo "=== $name ==="
    if "$@"; then
        STAGE_NAMES+=("$name")
        STAGE_RESULTS+=("PASSED")
    else
        STAGE_NAMES+=("$name")
        STAGE_RESULTS+=("FAILED")
    fi
}

if [ "$RUN_ALL" = true ]; then
    run_stage "All Tests" ./gradlew allTests
fi
if [ "$RUN_KT" = true ]; then
    run_stage "Kotlin Tests" ./gradlew :shared:testDebugUnitTest
fi
if [ "$RUN_IOS" = true ]; then
    # The iOS simulator test task only runs on a host whose CPU architecture
    # matches the simulator target - Apple Silicon Macs run the arm64
    # simulator, Intel Macs run the x64 simulator. Kotlin Gradle Plugin
    # silently disables (SKIPPED, not FAILED) whichever one doesn't match
    # the current host, regardless of Xcode/Kotlin version.
    if [ "$(uname -m)" = "arm64" ]; then
        IOS_TEST_TASK=":shared:iosSimulatorArm64Test"
    else
        IOS_TEST_TASK=":shared:iosX64Test"
    fi
    run_stage "iOS Tests" ./gradlew "$IOS_TEST_TASK"
fi
if [ "$RUN_ANDROID" = true ]; then
    run_stage "Android Tests" ./gradlew :composeApp:testDebugUnitTest
fi

echo ""
echo "=== Summary ==="
overall_exit_code=0
for i in "${!STAGE_NAMES[@]}"; do
    printf "%-15s %s\n" "${STAGE_NAMES[$i]}" "${STAGE_RESULTS[$i]}"
    if [ "${STAGE_RESULTS[$i]}" = "FAILED" ]; then
        overall_exit_code=1
    fi
done

exit $overall_exit_code
