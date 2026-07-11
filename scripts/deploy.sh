#!/usr/bin/env bash
# Deploy this project to your workshop server.
#
#   ./scripts/deploy.sh
#
# Packs the working tree (minus build artifacts), uploads it through the
# course MCP endpoint, and prints the build log. Reads the endpoint and
# bearer token from .mcp.json in the repo root — no arguments needed.
# Requires: bash, tar, base64, python3.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CFG="$ROOT/.mcp.json"
[ -f "$CFG" ] || { echo "error: $CFG not found — set up the MCP config first (see README)"; exit 1; }

ARCHIVE="$(mktemp)"
trap 'rm -f "$ARCHIVE"' EXIT
tar czf "$ARCHIVE" -C "$ROOT" \
  --exclude .git --exclude out --exclude target --exclude .mcp.json \
  --exclude legacy-java --exclude '*.jsonl' .

SIZE=$(wc -c < "$ARCHIVE")
[ "$SIZE" -le 8388608 ] || { echo "error: archive is $SIZE bytes (>8 MB cap) — clean build artifacts first"; exit 1; }

python3 - "$CFG" "$ARCHIVE" <<'PY'
import base64, json, sys, urllib.request

cfg_path, archive_path = sys.argv[1], sys.argv[2]
cfg = json.load(open(cfg_path))["mcpServers"]["mediatek"]
url, auth = cfg["url"], cfg["headers"]["Authorization"]
archive = base64.b64encode(open(archive_path, "rb").read()).decode()

rid = 0
def rpc(method, params=None, notify=False):
    global rid
    body = {"jsonrpc": "2.0", "method": method}
    if params is not None:
        body["params"] = params
    if not notify:
        rid += 1
        body["id"] = rid
    req = urllib.request.Request(url, method="POST", data=json.dumps(body).encode())
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json, text/event-stream")
    req.add_header("Authorization", auth)
    with urllib.request.urlopen(req, timeout=900) as resp:
        text = resp.read().decode()
    if notify:
        return None
    if "text/event-stream" in resp.headers.get("Content-Type", ""):
        result = None
        for line in text.splitlines():
            if line.startswith("data:"):
                try:
                    obj = json.loads(line[5:].strip())
                except ValueError:
                    continue
                if obj.get("id") == rid:
                    result = obj
        if result is None:
            raise SystemExit("error: no response for request %d" % rid)
    else:
        result = json.loads(text)
    if result.get("error"):
        raise SystemExit("deploy failed: %s" % json.dumps(result["error"]))
    return result["result"]

rpc("initialize", {"protocolVersion": "2025-03-26", "capabilities": {},
                   "clientInfo": {"name": "deploy.sh", "version": "1.0"}})
rpc("notifications/initialized", notify=True)
out = rpc("tools/call", {"name": "deployLocal", "arguments": {"archive": archive}})
for c in out.get("content", []):
    if c.get("type") == "text":
        print(c["text"])
sys.exit(1 if out.get("isError") else 0)
PY
