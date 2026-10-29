import sqlite3
import tempfile
import unittest
from pathlib import Path
from import_posts import import_drafts
from public_outages_api import initialize, get_outages
from publish_outage import publish

class PublishTests(unittest.TestCase):
    def test_requires_confirmation_and_verified_city(self):
        with tempfile.TemporaryDirectory() as folder:
            review = Path(folder) / "review.sqlite3"
            public = Path(folder) / "public.sqlite3"
            payload = {"schema_version":1,"publication_status":"draft","review_required":True,
              "posts":[{"source_id":"voda_dnr","post_id":"101",
                "source_url":"https://t.me/vodavdnr/101",
                "published_at":"2026-10-29T09:00:00+03:00","category":"water",
                "kind":"schedule","cities":["Макеевка","Донецк"],"text":"Пример графика",
                "requires_review":True}]}
            import_drafts(review,payload)
            initialize(public)
            def submit(city="Макеевка", confirmed=True):
                return publish(review,public,"voda_dnr","101",city,"planned",
                    ["Тестовый адрес"],"Тестовый источник",
                    "2026-10-29T10:00:00+03:00",confirmed)
            with self.assertRaises(ValueError):
                submit(confirmed=False)
            with self.assertRaises(ValueError):
                submit(city="Новоазовск")
            self.assertEqual(get_outages(public,"water","Макеевка"),[])
            self.assertTrue(submit())
            self.assertFalse(submit())
            self.assertEqual(len(get_outages(public,"water","Макеевка")),1)
            self.assertTrue(submit(city="Донецк"))
            self.assertFalse(submit(city="Донецк"))
            self.assertEqual(len(get_outages(public,"water","Донецк")),1)
            self.assertNotEqual(get_outages(public,"water","Донецк")[0]["id"],
                                get_outages(public,"water","Макеевка")[0]["id"])

if __name__ == "__main__":
    unittest.main()
