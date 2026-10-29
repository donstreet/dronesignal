"""Read-only local API for staged posts, intentionally separate from production API.

Usage: python3 review_api.py --db review.sqlite3 --port 8765
Bind loopback only; this is NOT an authenticated public/admin service.
"""
import argparse
import json
import sqlite3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit, parse_qs

CATEGORIES = {"water", "electricity", "mixed_utilities", "fuel", "news"}

def list_drafts(db_path, category=None, limit=50):
    if category is not None and category not in CATEGORIES:
        raise ValueError("Unknown category")
    if not 1 <= limit <= 100:
        raise ValueError("Invalid limit")
    if not db_path.exists():
        return []
    with sqlite3.connect(f"file:{db_path}?mode=ro", uri=True) as db:
        db.row_factory = sqlite3.Row
        sql = """SELECT source_id, post_id, source_url, published_at, category, kind,
                        cities_json, body, moderation_status FROM review_posts
                 WHERE moderation_status='draft'"""
        args = []
        if category:
            sql += " AND category=?"
            args.append(category)
        sql += " ORDER BY published_at DESC, source_id, post_id LIMIT ?"
        args.append(limit)
        try:
            rows = db.execute(sql, args).fetchall()
        except sqlite3.OperationalError as exc:
            if "no such table" in str(exc):
                return []
            raise
    return [{**dict(row), "cities": json.loads(row["cities_json"])} for row in rows]

def handler_for(db_path):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            url = urlsplit(self.path)
            if url.path != "/internal/review-posts":
                self.respond(404, {"error": "not_found"})
                return
            try:
                params = parse_qs(url.query)
                if set(params) - {"category", "limit"}:
                    raise ValueError("Unknown query")
                category = params.get("category", [None])[0]
                limit = int(params.get("limit", ["50"])[0])
                posts = list_drafts(db_path, category, limit)
            except (ValueError, sqlite3.DatabaseError):
                self.respond(400, {"error": "invalid_request"})
                return
            self.respond(200, {"status": "drafts_only", "posts": posts})
        def respond(self, code, data):
            raw = json.dumps(data, ensure_ascii=False).encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(raw)))
            self.end_headers()
            self.wfile.write(raw)
    return Handler

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--db", type=Path, required=True)
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    ThreadingHTTPServer(("127.0.0.1", args.port), handler_for(args.db)).serve_forever()
