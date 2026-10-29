"""One-shot ingestion into local moderation staging, never into public application data.

Usage: python3 pipeline.py --db /opt/dronesignal/review.sqlite3 --snapshot /opt/dronesignal/review-posts.json
"""
import argparse
import json
from pathlib import Path
from export_posts import export
from import_posts import import_drafts

def run(db: Path, snapshot: Path, collector=None):
    if collector is None:
        export(snapshot)
    else:
        export(snapshot, collector=collector)
    payload = json.loads(snapshot.read_text(encoding="utf-8"))
    staged = import_drafts(db, payload)
    return {"staged": staged, "published": 0, "notifications_sent": 0}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--db", required=True, type=Path)
    parser.add_argument("--snapshot", required=True, type=Path)
    args = parser.parse_args()
    print(json.dumps(run(args.db, args.snapshot), ensure_ascii=False))
