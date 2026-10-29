import sqlite3
import tempfile
import unittest
from pathlib import Path
from public_outages_api import initialize, get_outages

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
    def test_missing_database_is_not_an_empty_feed(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(sqlite3.OperationalError):
                get_outages(Path(temp) / "missing.sqlite3", "water", "Донецк")

if __name__ == "__main__":
    unittest.main()
