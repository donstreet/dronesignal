# Electricity API contract (pending server implementation)

Android screen requests `GET {HTTPS_BASE}/api/outages?kind=electricity&city={urlencoded city name}`.
The HTTPS base must be configured in the app before enabling live requests; it is intentionally blank until a verified production endpoint exists.

Successful response (HTTP 200):
```json
{"outages":[{"id":"source:post-id","city":"Макеевка","kind":"electricity","verification":"confirmed","status":"planned","addresses":["улица и дом"],"expected_restore_at":null,"source":"название источника","published_at":"2026-10-29T12:00:00+03:00"}]}
```
Example is a schema illustration, NOT an actual outage. Empty response: `{"outages":[]}`.

Server requirements:
- Public endpoint must expose only moderated/confirmed publications, never review drafts.
- Resolve geography from each source publication; do not broadcast unknown geography to all cities.
- Preserve source publication URL and fetched/verified timestamp in production schema.
- Validate status: planned, active, restored, unknown. No invented restoration time.
- Return explicit error codes on upstream failures, rather than a misleading empty list.
- Deduplicate source ID + post ID, support corrections and expiry.
- Use authenticated admin ingestion; HTTPS; rate limits; no secrets in APK.

Current status: Android client parsing exists, but production API and base URL are NOT connected or verified. Do not describe the electricity feature as live until end-to-end testing passes.
