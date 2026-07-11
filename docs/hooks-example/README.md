# Stop hook example

A minimal example of a Claude Code [`Stop`
hook](https://docs.claude.com/en/docs/claude-code/hooks) — a script the
agent runs on itself right before it finishes responding. This one just
prints a short reminder to double-check that whatever was just done was
actually verified, not merely assumed to work.

## Files

- `verify-before-done.sh` — the hook script. Advisory only (exits `0`), so
  it never blocks the session from ending; it just leaves a note.
- `settings.snippet.json` — the `hooks` block to merge into a project's
  `.claude/settings.json` (or `settings.local.json`) to wire it up.

## Using it

Merge the contents of `settings.snippet.json` into your `hooks` config,
then restart the session. `$CLAUDE_PROJECT_DIR` is set by Claude Code
itself, so the path resolves regardless of where the session is started
from.

## Making it a hard gate instead

Swap the script's `exit 0` for `exit 2`, which tells Claude Code to block
the stop and feed the script's stderr back to the agent as a reason to
keep going. Do this carefully: a Stop hook fires again every time the
agent tries to stop, so a script that unconditionally exits `2` will loop
forever. A real gate needs its own way to tell "already verified" from
"not yet" — for example, checking whether the last test run's exit code
was recorded somewhere in the session, or requiring the agent to write a
marker file after verification. This example leaves that part out on
purpose to keep the sample small; it's a starting point, not a policy.
