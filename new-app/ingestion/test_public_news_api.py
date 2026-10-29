import json
import sqlite3
import tempfile
import unittest
from pathlib import Path
from threading import Thread
from http.server import ThreadingHTTPServer
from urllib.request import urlopen
from urllib.error import HTTPError

from public_outages_api import initialize, get_news, make_handler

class PublicNewsTests(unittest.TestCase):
    def test_only_confirmed_visible_and_city_filtered(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "public.sqlite3"
            initialize(path)
            with sqlite3.connect(path) as db:
                db.execute("""INSERT INTO published_news
                    (id,city,title,summary,category,source,source_url,published_at,verified_at,verification,visible)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)""",
                    ("news-1","Горловка","Проверенный заголовок","Краткое сообщение","news",
                     "Администрация города","https://t.me/Gorlowka_now/42",
                     "2026-10-29T12:00:00+00:00","2026-10-29T12:05:00+00:00","confirmed",1))
                db.execute("""INSERT INTO published_news
                    (id,city,title,summary,category,source,source_url,published_at,verified_at,verification,visible)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)""",
                    ("news-2","Горловка","Скрытое сообщение","Не показывать","news",
                     "Источник","https://t.me/Gorlowka_now/43",
                     "2026-10-29T12:01:00+00:00","2026-10-29T12:05:00+00:00","confirmed",0))
            rows = get_news(path, "Горловка")
            self.assertEqual(len(rows), 1)
            self.assertEqual(rows[0]["title"], "Проверенный заголовок")
            self.assertEqual(get_news(path, "Донецк"), [])
            with self.assertRaises(ValueError):
                get_news(path, "Несуществующий город")
            with self.assertRaises(ValueError):
                get_news(path, "Горловка", 51)

    def test_http_news_feed_and_query_validation(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "public.sqlite3"
            initialize(path)
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(path))
            worker = Thread(target=server.serve_forever, daemon=True)
            worker.start()
            base = "http://127.0.0.1:%d" % server.server_port
            try:
                with urlopen(base + "/api/news?city=%D0%93%D0%BE%D1%80%D0%BB%D0%BE%D0%B2%D0%BA%D0%B0") as response:
                    payload = json.load(response)
                    self.assertEqual(response.status, 200)
                    self.assertEqual(payload["items"], [])
                    self.assertIn("updated_at", payload)
                for suffix in ("/api/news", "/api/news?city=Горловка&unexpected=1"):
                    with self.assertRaises(HTTPError) as caught:
                        urlopen(base + suffix)
                    self.assertEqual(caught.exception.code, 400)
            finally:
                server.shutdown()
                server.server_close()
                worker.join(timeout=3)

if __name__ == "__main__":
    unittest.main()
