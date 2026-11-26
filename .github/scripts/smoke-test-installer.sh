#!/usr/bin/env bash
# Installs a JMVS installer (.dmg, .msi or .deb), opens data/1crn.cif with --screenshot and checks that a PNG is
# written (quality scenario Q4, docs/arc42/10-quality-requirements.md).
# Usage: smoke-test-installer.sh <installer> <screenshot.png>
# GitHub's macOS runners have no hardware-accelerated OpenGL, which GLFW requires. With JMVS_SMOKE_ALLOW_NO_GL=1 a
# failure to get an OpenGL pixel format passes with a warning: by then the bundled runtime, the app and the LWJGL
# natives have all loaded.
set -euo pipefail

installer="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
screenshot=$2
structure="$PWD/data/1crn.cif"
rm -f "$screenshot"
runner=()

case "$installer" in
    *.dmg)
        mount=$(mktemp -d)
        hdiutil attach -quiet -nobrowse -readonly -mountpoint "$mount" "$installer"
        trap 'hdiutil detach -quiet "$mount" || true' EXIT
        app="$mount/JMVS.app/Contents/MacOS/JMVS"
        ;;
    *.msi)
        msiexec //i "$(cygpath -w "$installer")" //qn //norestart
        app="$(cygpath -u "$PROGRAMFILES")/JMVS/JMVS.exe"
        structure=$(cygpath -w "$structure")
        screenshot_arg=$(cygpath -w "$screenshot")
        ;;
    *.deb)
        sudo apt-get install -y "$installer"
        find /usr/local/share/applications /usr/share/applications -name "*jmvs*.desktop" 2>/dev/null | grep .
        app=/opt/jmvs/bin/JMVS
        runner=(xvfb-run -a -s "-screen 0 1280x800x24")
        ;;
    *)
        echo "Unknown installer type: $installer" >&2
        exit 2
        ;;
esac

echo "Running $app"
log=$(mktemp)
${runner[@]+"${runner[@]}"} "$app" "$structure" --screenshot "${screenshot_arg:-$screenshot}" > "$log" 2>&1 &
pid=$!
for _ in $(seq 120); do
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
done
if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    echo "JMVS did not exit within 120 s" >&2
    exit 1
fi
status=0
wait "$pid" || status=$?
cat "$log"
if [[ $status -ne 0 && "${JMVS_SMOKE_ALLOW_NO_GL:-}" == 1 ]] && grep -q "Failed to find a suitable pixel format" "$log"; then
    echo "::warning::No hardware OpenGL on this runner: JMVS started but could not create a window"
    exit 0
fi
if [[ $status -ne 0 ]]; then
    echo "JMVS exited with status $status" >&2
    exit "$status"
fi

size=$(wc -c < "$screenshot")
echo "Screenshot $screenshot: $size bytes"
if (( size < 10000 )); then
    echo "Screenshot is suspiciously small" >&2
    exit 1
fi
