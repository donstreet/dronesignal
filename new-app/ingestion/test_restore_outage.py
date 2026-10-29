import sqlite3
import tempfile
import unittest
from pathlib import Path
from public_outages_api import initialize, get_outages
from restore_outage import restore

class RestorationTests(unittest.TestCase):
    def test_exact_city_evidence_and_idempotence(self):
        with tempfile.TemporaryDirectory() as tmp:
            db = Path(tmp) / "public.sqlite3"
            initialize(db)
            with sqlite3.connect(db) as conn:
                for city in ("Донецк", "Макеевка"):
                    conn.execute("""INSERT INTO published_outages
                        (id,kind,city,verification,status,addresses_json,source,
                         source_url,published_at,verified_at,visible)
                        VALUES (?,?,?,?,?,?,?,?,?,?,1)""",
                        ("voda_dnr:101:" + city,"water",city,"confirmed","active",
                         '["Тестовый адрес"]',"Тестовый источник",
                         "https://t.me/vodavdnr/101","2026-10-29T09:00:00+03:00",
                         "2026-10-29T10:00:00+03:00"))
            def apply(city="Донецк", confirm=True, url="https://t.me/vodavdnr/102"):
                return restore(db,"voda_dnr:101:Донецк",city,url,
                               "2026-10-29T11:00:00+03:00",confirm)
            with self.assertRaises(ValueError):
                apply(confirm=False)
            with self.assertRaises(ValueError):
                apply(url="http://example.org")
            with self.assertRaises(ValueError):
                apply(city="Макеевка")
            with self.assertRaises(ValueError):
                restore(db,"voda_dnr:101:Донецк","Донецк",
                        "https://t.me/vodavdnr/102","2026-10-29T09:00:00+03:00",True)
            self.assertFalse(restore(db,"voda_dnr:999:Донецк","Донецк",
                                     "https://t.me/vodavdnr/102",
                                     "2026-10-29T11:00:00+03:00",True))
            self.assertTrue(apply())
            self.assertFalse(apply())
            self.assertEqual(get_outages(db,"water","Донецк")[0]["status"],"restored")
            self.assertEqual(get_outages(db,"water","Макеевка")[0]["status"],"active")
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT evidence_url FROM restoration_evidence").fetchone()[0],
                                 "https://t.me/vodavdnr/102")

if __name__ == "__main__":
    unittest.main()
