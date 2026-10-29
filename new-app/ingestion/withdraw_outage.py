"""Operator-only CLI for withdrawing a previously published outage.

This command has no network listener. Withdrawal is reversible only by a
separate reviewed workflow; drafts and other city records are unaffected.
"""
import argparse
import sqlite3
from pathlib import Path
from public_outages_api import CITIES

def withdraw(db_path: Path, source_id: str, post_id: str, city: str, confirm=False):
    if not confirm:
        raise ValueError("Explicit withdrawal confirmation required")
    if city not in CITIES or not source_id or not post_id:
        raise ValueError("Invalid publication identity")
    record_id = source_id + ":" + post_id + ":" + city
    with sqlite3.connect(db_path) as db:
        result = db.execute(
            "UPDATE published_outages SET visible=0 WHERE id=? AND city=? AND visible=1",
            (record_id, city))
        return result.rowcount == 1

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", type=Path, required=True)
    ap.add_argument("--source-id", required=True)
    ap.add_argument("--post-id", required=True)
    ap.add_argument("--city", choices=sorted(CITIES), required=True)
    ap.add_argument("--confirm", action="store_true")
    args = ap.parse_args()
    print("Withdrawn:", withdraw(args.db, args.source_id, args.post_id,
                                args.city, args.confirm))
