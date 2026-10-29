# City-filtered municipal news feed

The Android News screen now requests `GET /api/news?city=<selected-city>&limit=20` from the configured HTTPS API base (`utility_api_base`). The endpoint is implemented in `ingestion/public_outages_api.py`; its response contains `items` and `updated_at`.

Example response shape (illustration only, not real news):
```json
{"items":[{"id":"stable-id","city":"Горловка","title":"Publication heading","summary":"Short factual summary","category":"news","source":"Администрация города","source_url":"https://t.me/Gorlowka_now/42","published_at":"2026-10-29T12:00:00+00:00","verified_at":"2026-10-29T12:05:00+00:00","verification":"confirmed"}],"updated_at":"2026-10-29T12:06:00+00:00"}
```

The public endpoint serves only rows with `verification='confirmed'` and `visible=1`, filters by the selected city, and returns no moderation drafts. It is read-only over HTTP. The public database must be initialized with `public_outages_api.py --init-db` before use.

The Telegram collector now fetches the allowlisted municipal and utility channels in `ingestion/telegram_fetch.py`. Posts are deduplicated by channel/post ID and staged for review; source failures are reported in the snapshot. A city mentioned in text is only a classification hint, not a publication decision. All municipal news requires human review. MAX is not automatically collected because a public channel URL is not an API authorization.

**Not yet live:** the production server has not been deployed/configured in this repository, no verified news items are populated in the public database, and there is no authenticated moderator UI or automatic staging-to-public publication bridge. Do not configure the Android app to a production base URL until deployment, TLS, access controls, real data and empty/error/offline cases have been tested.
