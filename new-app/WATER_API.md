# Water API contract

Planned endpoint: `GET /api/outages?kind=water&city={urlencoded city}` over HTTPS. Only verified, moderated publications for the exact selected city may appear. This endpoint is NOT currently deployed or connected to the Android Water screen.

Illustrative response (not a real incident):
```json
{"outages":[{"id":"source:post-id","kind":"water","city":"Макеевка","verification":"confirmed","status":"planned","addresses":["Уточнённый адрес"],"supply_schedule":null,"expected_restore_at":null,"source":"Название источника","source_url":"https://example.org/post","published_at":"ISO-8601 timestamp","verified_at":"ISO-8601 timestamp"}]}
```

Statuses: planned, active, restored, unknown. Water delivery (`delivery`) must be a separate event type; never mislabel delivery as an outage. If the source gives no restoration time, return null. A source mentioning both Донецк and Макеевка must be reviewed for exact addresses before publication. Unknown geography stays in moderation. No publication means no confirmed reports, **not** guaranteed uninterrupted service.

Potential sources are in SOURCES.md. The local SQLite review API is not a public production feed. Implement authenticated moderation and production persistence, verify the existing server deployment and HTTPS base URL before enabling Android fetching. Do not embed server credentials in APK. Acceptance requires server contract tests, city filtering, failure/empty/offline UI and real-source end-to-end checks.
