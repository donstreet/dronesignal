import sqlite3
import tempfile
import unittest
from pathlib import Path
from public_outages_api import initialize, get_outages
from withdraw_outage import withdraw

class WithdrawTests(unittest.TestCase):
    def test_withdraw_only_requested_city_and_require_confirmation(self):
        with tempfile.TemporaryDirectory() as temp:
            db = Path(temp) / "public.sqlite3"
            initialize(db)
            with sqlite3.connect(db) as conn:
                for city in ("Донецк", "Макеевка"):
                    conn.execute("""INSERT INTO published_outages
                        (id,kind,city,verification,status,addresses_json,source,
                         source_url,published_at,verified_at,visible)
                        VALUES (?,?,?,?,?,?,?,?,?,?,1)""",
                        ("voda_dnr:101:" + city, "water", city, "confirmed", "active",
                         '["Тестовый адрес"]', "Тестовый источник",
                         "https://t.me/vodavdnr/101", "2026-10-29T09:00:00+03:00",
                         "2026-10-29T10:00:00+03:00"))
            with self.assertRaises(ValueError):
                withdraw(db, "voda_dnr", "101", "Донецк")
            self.assertTrue(withdraw(db, "voda_dnr", "101", "Донецк", True))
            self.assertFalse(withdraw(db, "voda_dnr", "101", "Донецк", True))
            self.assertEqual(get_outages(db, "water", "Донецк"), [])
            self.assertEqual(len(get_outages(db, "water", "Макеевка")), 1)
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM published_outages").fetchone()[0], 2)

if __name__ == "__main__":
    unittest.main()
