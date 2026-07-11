# mediatek-sample-backend

Starter repo for the Claude Code × MediaTek workshop. A small in-memory
Java backend for an internal reporting tool — create a report, then export
it as plain text.

## Deploy (workshop)

```sh
./scripts/deploy.sh
```

Packs the repo and deploys it to your workshop server via the course MCP
endpoint. Needs `.mcp.json` in the repo root (see below) and `python3`.

## Run

```sh
./build.sh
PORT=8080 java -cp out tw.winlab.reportlab.Main
```

## Auth

Send `Authorization: Bearer test-<your-id>` on any endpoint that needs a
caller identity (e.g. `test-alice` → user `alice`). Admin endpoints
(`/admin/...`) require `Bearer test-admin`.

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/health` | — | liveness check |
| GET | `/me` | any user | echoes the caller's userId |
| POST | `/reports` | any user | create a report `{title, periodStart, periodEnd, rows}` |
| GET | `/reports` | any user | list your own reports (admin sees all) |
| GET | `/reports/:id` | owner or admin | report detail, including rows |
| DELETE | `/reports/:id` | owner or admin | delete a report |
| POST | `/reports/:id/export` | owner or admin | export the report as text; `?format=full` (default, includes totals) or `?format=brief` |
| GET | `/admin/stats` | admin | totals across reports and export attempts |
| GET | `/admin/export-log` | admin | recent export attempts (report, period, format, success) |

## Report shape

```json
{
  "title": "Weekly Sales",
  "periodStart": "2026-07-14",
  "periodEnd": "2026-07-16",
  "rows": [
    {"region": "north", "revenue": 1000},
    {"region": "south", "revenue": 2000}
  ]
}
```

`periodStart` and `periodEnd` (`yyyy-MM-dd`) describe the period the report
covers and appear in the exported document's header. `periodEnd` must not
be before `periodStart`. The older single-`date` shape is no longer
accepted — a request that still sends `date` gets a `400` explaining the
new fields.

## Extras

`docs/hooks-example/` has a minimal Claude Code Stop hook you can copy
into your own project settings — see that directory's README.
