"""Read-only public feed for verified outages and published municipal news.

Moderation drafts are never served. Bind to loopback and expose only behind the configured
HTTPS reverse proxy. News publication must be performed by a trusted server-side process.
"""
import json
import sqlite3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit, parse_qs
from pathlib import Path
from datetime import datetime, timezone
import argparse

CITIES = {"Донецк", "Макеевка"}
NEWS_CITIES = {
    "Донецк", "Макеевка", "Дебальцево", "Докучаевск", "Горловка", "Енакиево",
    "Иловайск", "Мариуполь", "Снежное", "Торез (Чистяково)", "Харцызск",
    "Амвросиевка", "Волноваха", "Шахтёрск", "Новоазовск", "Тельманово",
    "Старобешево", "Володарское", "Мангуш", "Ждановка", "Кировское (Крестовка)",
    "Ясиноватая", "Красный Лиман",
}
KINDS = {"water", "electricity"}

def initialize(db_path):
    with sqlite3.connect(db_path) as db:
        db.execute("""CREATE TABLE IF NOT EXISTS published_outages (
            id TEXT PRIMARY KEY, kind TEXT NOT NULL, city TEXT NOT NULL,
            verification TEXT NOT NULL CHECK(verification='confirmed'),
            status TEXT NOT NULL, addresses_json TEXT NOT NULL,
            expected_restore_at TEXT, source TEXT NOT NULL,
            source_url TEXT NOT NULL, published_at TEXT NOT NULL,
            verified_at TEXT NOT NULL, visible INTEGER NOT NULL DEFAULT 1)""")
        db.execute("""CREATE TABLE IF NOT EXISTS published_news (
            id TEXT PRIMARY KEY, city TEXT NOT NULL, title TEXT NOT NULL,
            summary TEXT NOT NULL, category TEXT NOT NULL DEFAULT 'news',
            source TEXT NOT NULL, source_url TEXT NOT NULL,
            published_at TEXT NOT NULL, verified_at TEXT NOT NULL,
            verification TEXT NOT NULL CHECK(verification='confirmed'),
            visible INTEGER NOT NULL DEFAULT 1)""")

def get_outages(db_path, kind, city):
    if kind not in KINDS or city not in CITIES:
        raise ValueError("Unsupported city or kind")
    with sqlite3.connect(f"file:{db_path.resolve()}?mode=ro", uri=True) as db:
        db.row_factory = sqlite3.Row
        rows = db.execute("""SELECT id,kind,city,verification,status,addresses_json,
            expected_restore_at,source,source_url,published_at,verified_at
            FROM published_outages WHERE kind=? AND city=? AND verification='confirmed'
            AND visible=1 ORDER BY published_at DESC LIMIT 50""", (kind, city)).fetchall()
    return [{**{k: r[k] for k in r.keys() if k != "addresses_json"},
             "addresses": json.loads(r["addresses_json"])} for r in rows]

def get_news(db_path, city, limit=20):
    if city not in NEWS_CITIES or not 1 <= limit <= 50:
        raise ValueError("Unsupported city or limit")
    with sqlite3.connect(f"file:{db_path.resolve()}?mode=ro", uri=True) as db:
        db.row_factory = sqlite3.Row
        rows = db.execute("""SELECT id,city,title,summary,category,source,source_url,
            published_at,verified_at,verification FROM published_news
            WHERE city=? AND verification='confirmed' AND visible=1
            ORDER BY published_at DESC LIMIT ?""", (city, limit)).fetchall()
    return [dict(row) for row in rows]

def make_handler(db_path):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            url = urlsplit(self.path)
            args = parse_qs(url.query)
            if url.path == "/api/outages":
                if set(args) != {"city", "kind"} or any(len(v) != 1 for v in args.values()):
                    return self.reply(400, {"error": "invalid_query"})
                try:
                    data = get_outages(db_path, args["kind"][0], args["city"][0])
                except ValueError:
                    return self.reply(400, {"error": "invalid_filter"})
                except (OSError, sqlite3.DatabaseError):
                    return self.reply(503, {"error": "feed_unavailable"})
                return self.reply(200, {"outages": data})
            if url.path == "/api/news":
                if not set(args).issubset({"city", "limit"}) or "city" not in args or any(len(v) != 1 for v in args.values()):
                    return self.reply(400, {"error": "invalid_query"})
                try:
                    limit = int(args.get("limit", ["20"])[0])
                    data = get_news(db_path, args["city"][0], limit)
                except ValueError:
                    return self.reply(400, {"error": "invalid_filter"})
                except (OSError, sqlite3.DatabaseError):
                    return self.reply(503, {"error": "feed_unavailable"})
                return self.reply(200, {"items": data, "updated_at": datetime.now(timezone.utc).isoformat()})
            return self.reply(404, {"error": "not_found"})
        def reply(self, status, payload):
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
    return Handler

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--db", type=Path, required=True)
    parser.add_argument("--port", type=int, default=8766)
    parser.add_argument("--init-db", action="store_true")
    args = parser.parse_args()
    if args.init_db:
        initialize(args.db)
    else:
        ThreadingHTTPServer(("127.0.0.1", args.port), make_handler(args.db)).serve_forever()
