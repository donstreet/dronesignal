import sqlite3
import tempfile
import unittest
from pathlib import Path
from public_outages_api import initialize, get_outages, make_handler
from http.server import ThreadingHTTPServer
from threading import Thread
from urllib.request import urlopen
from urllib.error import HTTPError
import json

class PublicOutagesTests(unittest.TestCase):
    def test_only_confirmed_visible_and_matching_city(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "published.sqlite3"
            initialize(path)
            with sqlite3.connect(path) as db:
                db.execute("""INSERT INTO published_outages
                    (id,kind,city,verification,status,addresses_json,source,source_url,published_at,verified_at,visible)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)""",
                    ("example-1","water","Макеевка","confirmed","planned",'["Пример адреса"]',
                     "example","https://example.org/post","2026-10-29T12:00:00+03:00",
                     "2026-10-29T12:05:00+03:00",1))
            rows = get_outages(path, "water", "Макеевка")
            self.assertEqual(len(rows), 1)
            self.assertEqual(rows[0]["addresses"], ["Пример адреса"])
            self.assertEqual(get_outages(path, "water", "Донецк"), [])
            self.assertEqual(get_outages(path, "electricity", "Макеевка"), [])
            with self.assertRaises(ValueError):
                get_outages(path, "water", "Другой город")
    def test_http_filter_and_errors(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "public.sqlite3"
            initialize(path)
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(path))
            worker = Thread(target=server.serve_forever, daemon=True)
            worker.start()
            base = "http://127.0.0.1:%d" % server.server_port
            try:
                with urlopen(base + "/api/outages?kind=water&city=%D0%94%D0%BE%D0%BD%D0%B5%D1%86%D0%BA") as response:
                    self.assertEqual(response.status, 200)
                    self.assertEqual(json.load(response), {"outages": []})
                for suffix, expected in [("/api/outages?kind=water", 400),
                                         ("/api/outages?kind=fuel&city=%D0%94%D0%BE%D0%BD%D0%B5%D1%86%D0%BA", 400),
                                         ("/internal/review-posts", 404)]:
                    with self.assertRaises(HTTPError) as caught:
                        urlopen(base + suffix)
                    self.assertEqual(caught.exception.code, expected)
            finally:
                server.shutdown()
                server.server_close()
                worker.join(timeout=3)

    def test_missing_database_is_not_an_empty_feed(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(sqlite3.OperationalError):
                get_outages(Path(temp) / "missing.sqlite3", "water", "Донецк")

if __name__ == "__main__":
    unittest.main()
