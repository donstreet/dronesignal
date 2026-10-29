"""Idempotent SQLite staging import. This does NOT publish posts or send notifications.

Usage: python import_posts.py --input review-posts.json --db staging.sqlite3
"""
import argparse
import json
import sqlite3
from pathlib import Path
from urllib.parse import urlparse

ALLOWED = {"makeevka_news": "Novosti_Makeevka", "fuel_dnr": "fueldnr",
           "benz_dnr": "benz_dnr", "voda_dnr": "vodavdnr"}
CATEGORIES = {"water", "electricity", "mixed_utilities", "fuel", "news"}
KINDS = {"delivery", "restored", "schedule", "outage_notice", "general"}

def validate(payload):
    if payload.get("schema_version") != 1 or payload.get("publication_status") != "draft" or payload.get("review_required") is not True:
        raise ValueError("Only schema v1 review drafts are accepted")
    posts = payload.get("posts")
    if not isinstance(posts, list) or len(posts) > 1000:
        raise ValueError("Invalid post collection")
    checked = []
    for p in posts:
        if not isinstance(p, dict):
            raise ValueError("Invalid post")
        source = p.get("source_id")
        post_id = str(p.get("post_id", ""))
        expected = ALLOWED.get(source)
        if not expected or not post_id.isascii() or not post_id.isdecimal() or len(post_id) > 20:
            raise ValueError("Unapproved source or post ID")
        url = p.get("source_url")
        parsed = urlparse(url) if isinstance(url, str) else None
        if not parsed or parsed.scheme != "https" or parsed.netloc != "t.me" or parsed.path != f"/{expected}/{post_id}" or parsed.query or parsed.fragment:
            raise ValueError("Source URL does not match channel and post")
        if p.get("category") not in CATEGORIES or p.get("kind") not in KINDS:
            raise ValueError("Unknown classification")
        if not isinstance(p.get("text"), str) or not p["text"].strip() or len(p["text"]) > 30000:
            raise ValueError("Invalid text")
        if not isinstance(p.get("cities"), list) or not all(isinstance(c, str) and len(c) <= 100 for c in p["cities"]):
            raise ValueError("Invalid cities")
        checked.append(p)
    return checked

def import_drafts(db_path: Path, payload):
    posts = validate(payload)  # Validate entire batch before any DB writes.
    with sqlite3.connect(db_path) as db:
        db.execute("""CREATE TABLE IF NOT EXISTS review_posts (
            source_id TEXT NOT NULL, post_id TEXT NOT NULL, source_url TEXT NOT NULL,
            published_at TEXT, category TEXT NOT NULL, kind TEXT NOT NULL,
            cities_json TEXT NOT NULL, body TEXT NOT NULL,
            requires_review INTEGER NOT NULL, moderation_status TEXT NOT NULL DEFAULT 'draft',
            PRIMARY KEY(source_id, post_id))""")
        for p in posts:
            db.execute("""INSERT INTO review_posts
                (source_id, post_id, source_url, published_at, category, kind,
                 cities_json, body, requires_review)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
                ON CONFLICT(source_id, post_id) DO UPDATE SET
                  source_url=excluded.source_url, published_at=excluded.published_at,
                  category=excluded.category, kind=excluded.kind,
                  cities_json=excluded.cities_json, body=excluded.body,
                  requires_review=1
                WHERE review_posts.moderation_status='draft'""",
                (p["source_id"], str(p["post_id"]), p["source_url"], p.get("published_at"),
                 p["category"], p["kind"], json.dumps(p["cities"], ensure_ascii=False), p["text"]))
    return len(posts)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--db", type=Path, required=True)
    args = parser.parse_args()
    print("Staged:", import_drafts(args.db, json.loads(args.input.read_text(encoding="utf-8"))))
