"""Publish one explicitly reviewed utility draft into a separate public database.

This command requires an operator to supply exact source ID, post ID, city,
status, source name, addresses and confirmation flag. No automatic publishing.
"""
import argparse
import json
import sqlite3
from pathlib import Path
from urllib.parse import urlsplit
from datetime import datetime
from public_outages_api import initialize, CITIES, KINDS

STATUSES = {"planned", "active", "restored", "unknown"}

def publish(review_db, public_db, source_id, post_id, city, status, addresses,
            source_name, verified_at, confirmed=False):
    if not confirmed:
        raise ValueError("Explicit operator confirmation required")
    if city not in CITIES or status not in STATUSES or not source_name.strip():
        raise ValueError("Invalid publication metadata")
    if not isinstance(addresses, list) or not addresses or not all(
            isinstance(a, str) and a.strip() and len(a) <= 250 for a in addresses):
        raise ValueError("Verified addresses are required")
    dt = datetime.fromisoformat(verified_at)
    if dt.tzinfo is None or dt.utcoffset() is None:
        raise ValueError("Verification timestamp requires timezone")
    with sqlite3.connect(f"file:{review_db.resolve()}?mode=ro", uri=True) as db:
        db.row_factory = sqlite3.Row
        row = db.execute("""SELECT * FROM review_posts
            WHERE source_id=? AND post_id=? AND moderation_status='draft'""",
            (source_id, post_id)).fetchone()
    if row is None:
        raise ValueError("Review draft not found")
    if row["category"] not in KINDS or row["kind"] == "delivery":
        raise ValueError("Not an outage publication")
    if city not in json.loads(row["cities_json"]) or not row["published_at"]:
        raise ValueError("City or source timestamp not verified")
    url = urlsplit(row["source_url"])
    if url.scheme != "https" or url.hostname != "t.me":
        raise ValueError("Invalid source URL")
    if not public_db.exists():
        raise FileNotFoundError("Initialize public DB separately before publishing")
    with sqlite3.connect(public_db) as db:
        db.execute("""INSERT INTO published_outages
            (id,kind,city,verification,status,addresses_json,expected_restore_at,
             source,source_url,published_at,verified_at,visible)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,1)
            ON CONFLICT(id) DO NOTHING""",
            (source_id + ":" + post_id + ":" + city, row["category"], city, "confirmed", status,
             json.dumps(addresses, ensure_ascii=False), None, source_name,
             row["source_url"], row["published_at"], verified_at))
        return db.total_changes == 1

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--review-db", required=True, type=Path)
    ap.add_argument("--public-db", required=True, type=Path)
    ap.add_argument("--source-id", required=True)
    ap.add_argument("--post-id", required=True)
    ap.add_argument("--city", required=True, choices=sorted(CITIES))
    ap.add_argument("--status", required=True, choices=sorted(STATUSES))
    ap.add_argument("--address", action="append", required=True)
    ap.add_argument("--source-name", required=True)
    ap.add_argument("--verified-at", required=True)
    ap.add_argument("--confirm", action="store_true")
    args = ap.parse_args()
    print("Published:", publish(args.review_db, args.public_db, args.source_id,
          args.post_id, args.city, args.status, args.address, args.source_name,
          args.verified_at, args.confirm))
