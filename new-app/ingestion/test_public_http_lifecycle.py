"""Verify public HTTP visibility changes across restoration and withdrawal."""
import json
import sqlite3
import tempfile
import threading
import unittest
from http.server import ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import urlopen
from public_outages_api import initialize, make_handler
from restore_outage import restore
from withdraw_outage import withdraw

class PublicHttpLifecycleTests(unittest.TestCase):
    def test_status_transitions_over_http(self):
        with tempfile.TemporaryDirectory() as folder:
            db = Path(folder) / "feed.sqlite3"
            initialize(db)
            with sqlite3.connect(db) as conn:
                conn.execute("""INSERT INTO published_outages
                    (id,kind,city,verification,status,addresses_json,source,
                     source_url,published_at,verified_at,visible)
                    VALUES (?,?,?,?,?,?,?,?,?,?,1)""",
                    ("source:1:Донецк","water","Донецк","confirmed","active",
                     '["Тестовый адрес"]',"Тестовый источник",
                     "https://t.me/example/1","2026-10-29T09:00:00+03:00",
                     "2026-10-29T10:00:00+03:00"))
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(db))
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            def fetch():
                query = urlencode({"kind":"water","city":"Донецк"})
                with urlopen("http://127.0.0.1:%d/api/outages?%s" %
                             (server.server_port, query), timeout=3) as response:
                    return json.load(response)["outages"]
            try:
                self.assertEqual(fetch()[0]["status"],"active")
                self.assertTrue(restore(db,"source:1:Донецк","Донецк",
                    "https://t.me/example/2","2026-10-29T11:00:00+03:00",True))
                self.assertEqual(fetch()[0]["status"],"restored")
                self.assertTrue(withdraw(db,"source","1","Донецк",True))
                self.assertEqual(fetch(),[])
            finally:
                server.shutdown()
                server.server_close()
                thread.join(timeout=3)

if __name__ == "__main__":
    unittest.main()
