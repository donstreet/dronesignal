# Shared news feed: implementation contract

Selected source: https://max.ru/vesti . The News tab is independent of the selected city. Current Android build opens the source externally; in-app automatic loading is **not implemented**.

Before implementation, verify an officially permitted method of reading channel posts, including authorization, rate limits, and redistribution conditions. Do not assume that a public MAX channel URL is a machine-readable API endpoint or embed private credentials in the APK.

Proposed server-side normalized response: `GET /api/news?feed=vesti&limit=20`:
```json
{"items":[{"id":"stable-source-post-id","title":"Publication heading","summary":"Short factual summary","published_at":"ISO-8601 timestamp","source":"Вести","source_url":"https://max.ru/vesti","verification":"confirmed"}],"updated_at":"ISO-8601 timestamp"}
```
This is a schema illustration, not real news. Only show verified fetched items; no generated articles, no fabricated timestamps. Preserve exact per-post URLs when available. Deduplicate by stable post ID, handle deletions/corrections, show source and freshness, distinguish an empty feed from a fetch error. Use a server-side cache, timeouts, and source attribution.

Acceptance: source method verified, server endpoint tested with real permitted data, Android endpoint configured over HTTPS, empty/error/offline states tested, and the selected city does not affect results.
