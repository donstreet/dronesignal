"""Manually confirm restoration of one exact published utility incident.

No automatic interpretation of source text and no public write endpoint.
"""
import argparse
import sqlite3
from datetime import datetime
from pathlib import Path
from urllib.parse import urlsplit
from public_outages_api import CITIES

def restore(db_path: Path, record_id: str, city: str, evidence_url: str,
            verified_at: str, confirm=False):
    if not confirm:
        raise ValueError("Explicit confirmation required")
    if city not in CITIES or not record_id or not record_id.endswith(":" + city):
        raise ValueError("Invalid incident identity")
    url = urlsplit(evidence_url)
    if url.scheme != "https" or not url.hostname or url.username or url.password:
        raise ValueError("HTTPS evidence URL required")
    timestamp = datetime.fromisoformat(verified_at)
    if timestamp.tzinfo is None or timestamp.utcoffset() is None:
        raise ValueError("Timezone required")
    with sqlite3.connect(db_path) as db:
        db.execute("""CREATE TABLE IF NOT EXISTS restoration_evidence (
            incident_id TEXT PRIMARY KEY, evidence_url TEXT NOT NULL,
            verified_at TEXT NOT NULL)""")
        row = db.execute("""SELECT status,visible,verified_at FROM published_outages
                            WHERE id=? AND city=?""", (record_id, city)).fetchone()
        if row is None or row[1] != 1 or row[0] not in ("active", "planned", "unknown"):
            return False
        previous = datetime.fromisoformat(row[2])
        if timestamp < previous:
            raise ValueError("Restoration verification predates outage verification")
        db.execute("""UPDATE published_outages SET status='restored',
            verified_at=? WHERE id=? AND city=? AND visible=1""",
            (verified_at, record_id, city))
        db.execute("""INSERT INTO restoration_evidence
            (incident_id,evidence_url,verified_at) VALUES (?,?,?)""",
            (record_id,evidence_url,verified_at))
        return True

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", type=Path, required=True)
    ap.add_argument("--incident-id", required=True)
    ap.add_argument("--city", choices=sorted(CITIES), required=True)
    ap.add_argument("--evidence-url", required=True)
    ap.add_argument("--verified-at", required=True)
    ap.add_argument("--confirm", action="store_true")
    a = ap.parse_args()
    print("Restored:", restore(a.db,a.incident_id,a.city,a.evidence_url,
                               a.verified_at,a.confirm))
