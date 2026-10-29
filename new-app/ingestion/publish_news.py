"""Publish one reviewed municipal news draft to the read-only public feed.

This is an operator-only CLI, not an HTTP write endpoint. The operator must choose the
city, write/approve the title and summary, and pass --confirm after checking the source.
"""
import argparse
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

from import_posts import ALLOWED
from public_outages_api import NEWS_CITIES, initialize

def publish_news(review_db: Path, public_db: Path, source_id: str, post_id: str,
                 city: str, title: str, summary: str, confirmed: bool):
    if not confirmed:
        raise ValueError("Explicit --confirm is required after human review")
    if city not in NEWS_CITIES:
        raise ValueError("Unsupported city")
    title = title.strip()
    summary = summary.strip()
    if not title or len(title) > 180:
        raise ValueError("Title must contain 1-180 characters")
    if not summary or len(summary) > 3000:
        raise ValueError("Summary must contain 1-3000 characters")
    channel = ALLOWED.get(source_id)
    if not channel or not post_id.isascii() or not post_id.isdecimal() or len(post_id) > 20:
        raise ValueError("Unknown source or invalid post ID")
    expected_url = f"https://t.me/{channel}/{post_id}"

    with sqlite3.connect(f"file:{review_db.resolve()}?mode=ro", uri=True) as review:
        review.row_factory = sqlite3.Row
        try:
            draft = review.execute("""SELECT source_url,published_at,body,moderation_status
                FROM review_posts WHERE source_id=? AND post_id=?""",
                (source_id, post_id)).fetchone()
        except sqlite3.OperationalError as exc:
            raise ValueError("Review database is missing or not initialized") from exc
    if draft is None or draft["moderation_status"] != "draft":
        raise ValueError("Only an existing unmoderated draft can be published")
    if draft["source_url"] != expected_url:
        raise ValueError("Draft source URL does not match the allowlisted channel")
    published_at = draft["published_at"]
    try:
        parsed_date = datetime.fromisoformat(published_at)
        if parsed_date.tzinfo is None or parsed_date.utcoffset() is None:
            raise ValueError
    except (TypeError, ValueError) as exc:
        raise ValueError("Draft has no valid timezone-aware publication timestamp") from exc

    initialize(public_db)
    verified_at = datetime.now(timezone.utc).isoformat()
    news_id = f"{source_id}:{post_id}"
    with sqlite3.connect(public_db) as public:
        public.execute("""INSERT INTO published_news
            (id,city,title,summary,category,source,source_url,published_at,verified_at,verification,visible)
            VALUES (?,?,?,?,?,?,?,?,?,'confirmed',1)
            ON CONFLICT(id) DO NOTHING""",
            (news_id, city, title, summary, "news", source_id, expected_url,
             published_at, verified_at))
    # If this update fails, the public insert is idempotent; rerunning is safe.
    with sqlite3.connect(review_db) as review:
        review.execute("""UPDATE review_posts SET moderation_status='published'
            WHERE source_id=? AND post_id=? AND moderation_status='draft'""",
            (source_id, post_id))
    return news_id

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--review-db", type=Path, required=True)
    parser.add_argument("--public-db", type=Path, required=True)
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--post-id", required=True)
    parser.add_argument("--city", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--summary", required=True)
    parser.add_argument("--confirm", action="store_true",
                        help="Confirm the original post, city, title and summary were manually verified")
    args = parser.parse_args()
    print(publish_news(args.review_db, args.public_db, args.source_id, args.post_id,
                       args.city, args.title, args.summary, args.confirm))
