#!/usr/bin/env bash
# Stop hook: intercept the agent once, right as it claims to be done, and
# make it state the verification status before the session may end.
#
# Claude Code runs Stop hooks each time the agent is about to finish
# responding. Emitting {"decision":"block"} feeds `reason` back to the
# agent, which must respond to it. The stop_hook_active guard lets the
# session end on the following stop — without it this would loop forever.
set -euo pipefail

INPUT="$(cat)"
if printf '%s' "$INPUT" | grep -q '"stop_hook_active":[[:space:]]*true'; then
  exit 0
fi

cat <<'JSON'
{"decision": "block", "reason": "收工前的驗收檢查:這次的修改 deploy 了嗎?Run probes 跑了嗎、全綠嗎?把驗收狀態講清楚再收工——還沒驗,就先去驗。"}
JSON
