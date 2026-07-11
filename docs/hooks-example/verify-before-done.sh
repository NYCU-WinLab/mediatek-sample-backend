#!/usr/bin/env bash
# Stop hook: a small nudge before the session ends.
#
# Claude Code runs every hook registered under "Stop" each time the agent
# is about to finish responding. This one doesn't inspect the conversation
# at all — it just prints a reminder to stderr so it shows up wherever the
# session's hook output is surfaced.
#
# Exit 0 keeps this advisory-only: it never blocks the session from
# ending. If you want a hard gate instead, exit 2 and add your own check
# for whether verification already happened — otherwise every Stop event
# re-triggers the same block and the session can never end.
set -euo pipefail

cat >&2 <<'EOF'
[verify-before-done] Before calling this done: did you actually run the
build / tests / whatever proves it works, and look at the output — not
just assume it's fine?
EOF

exit 0
