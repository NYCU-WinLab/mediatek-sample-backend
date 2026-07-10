# mediatek-sample-backend

Starter repo for the Claude Code × MediaTek workshop. A small in-memory
Java backend for an internal reporting tool — create a report, then export
it as plain text.

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
| POST | `/reports` | any user | create a report `{title, date, rows}` |
| GET | `/reports` | any user | list your own reports (admin sees all) |
| GET | `/reports/:id` | owner or admin | report detail, including rows |
| DELETE | `/reports/:id` | owner or admin | delete a report |
| POST | `/reports/:id/export` | owner or admin | export the report as text; `?format=full` (default, includes totals) or `?format=brief` |
| GET | `/admin/stats` | admin | totals across reports and export attempts |
| GET | `/admin/export-log` | admin | recent export attempts (report, date, format, success) |

## Report shape

```json
{
  "title": "Weekly Sales",
  "date": "2026-07-14",
  "rows": [
    {"region": "north", "revenue": 1000},
    {"region": "south", "revenue": 2000}
  ]
}
```

`date` is the report's period (`yyyy-MM-dd`) and appears in the exported
document's header.
