"""End-to-end lifecycle of an approved utility incident in a disposable database."""
import sqlite3
import tempfile
import unittest
from pathlib import Path
from public_outages_api import initialize, get_outages
from restore_outage import restore
from withdraw_outage import withdraw

class LifecycleTests(unittest.TestCase):
    def test_public_feed_restoration_and_withdrawal(self):
        with tempfile.TemporaryDirectory() as folder:
            db = Path(folder) / "public.sqlite3"
            initialize(db)
            with sqlite3.connect(db) as conn:
                conn.execute("""INSERT INTO published_outages
                    (id,kind,city,verification,status,addresses_json,source,
                     source_url,published_at,verified_at,visible)
                    VALUES (?,?,?,?,?,?,?,?,?,?,1)""",
                    ("power:42:Донецк","electricity","Донецк","confirmed","active",
                     '["Тестовый адрес"]',"Тестовый источник",
                     "https://t.me/example/42","2026-10-29T09:00:00+03:00",
                     "2026-10-29T10:00:00+03:00"))
            self.assertEqual(get_outages(db,"electricity","Донецк")[0]["status"],"active")
            self.assertEqual(get_outages(db,"water","Донецк"),[])
            self.assertTrue(restore(db,"power:42:Донецк","Донецк",
                "https://t.me/example/43","2026-10-29T11:00:00+03:00",True))
            feed = get_outages(db,"electricity","Донецк")
            self.assertEqual(len(feed),1)
            self.assertEqual(feed[0]["status"],"restored")
            self.assertTrue(withdraw(db,"power","42","Донецк",True))
            self.assertEqual(get_outages(db,"electricity","Донецк"),[])
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute(
                    "SELECT status,visible FROM published_outages WHERE id=?",
                    ("power:42:Донецк",)).fetchone(),("restored",0))
                self.assertEqual(conn.execute(
                    "SELECT evidence_url FROM restoration_evidence WHERE incident_id=?",
                    ("power:42:Донецк",)).fetchone()[0],"https://t.me/example/43")

if __name__ == "__main__":
    unittest.main()
