# Deployment checklist: utility feeds (not deployed yet)

Run from `new-app/ingestion`. Python 3.12+, SQLite, and an HTTPS reverse proxy are required. Keep the moderation and public databases in separate, access-restricted paths. Do not expose the local moderation API or SQLite files on the internet.

1. Run `python -m unittest discover -v` and require a green CI result.
2. Back up existing server data. Initialize a **new** public database with `python public_outages_api.py --db /secure/path/public.sqlite3 --init-db`. Never use the review database here.
3. Import source posts into the separate review database. Independently verify city, addresses, time, source, current status and publication rights before using `publish_outage.py`. Test entries must never be published as real incidents.
4. Start the public API on loopback: `python public_outages_api.py --db /secure/path/public.sqlite3 --port 8766`. Put only the required read-only routes (`/api/outages` and `/api/news`) behind the HTTPS reverse proxy, with monitoring and rate limits. Do not expose the local moderation API or write routes. The service has no public write endpoint.
5. Check `/api/outages?kind=water&city=...` and `kind=electricity` for both supported cities, plus `/api/news?city=...&limit=20` for a supported municipality. Test empty feeds, 400 invalid filters, 503 missing DB and confirmed sample records only in a disposable test environment.
6. For corrections, use `withdraw_outage.py` with explicit confirmation. For verified restoration, use `restore_outage.py` with an exact incident ID, city, source evidence URL and verification timestamp.
7. Only after production API tests, configure the Android Activity preferences key `utility_api_base` to the verified HTTPS origin (no trailing `/api/outages` path); both Water and Electricity use it. This key is intentionally empty in distributed builds. Test offline, slow response and stale data states.

Known limits: no production deployment, automated collector-to-public news publication, authenticated moderator UI, or real live utility/news data is claimed. The public database is managed via operator-only CLI; restrict filesystem and process access accordingly.
