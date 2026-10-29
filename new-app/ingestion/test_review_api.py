import tempfile
import unittest
from pathlib import Path
from import_posts import import_drafts
from review_api import list_drafts

class ReviewApiTests(unittest.TestCase):
    def test_only_drafts_and_category_filter(self):
        with tempfile.TemporaryDirectory() as tmp:
            db = Path(tmp) / "staging.sqlite3"
            payload = {"schema_version": 1, "publication_status": "draft",
                       "review_required": True, "posts": [
                {"source_id": "voda_dnr", "post_id": "10",
                 "source_url": "https://t.me/vodavdnr/10", "published_at": "2026-10-29",
                 "category": "water", "kind": "schedule", "cities": ["Макеевка"],
                 "text": "График воды"}]}
            import_drafts(db, payload)
            rows = list_drafts(db, category="water")
            self.assertEqual(len(rows), 1)
            self.assertEqual(rows[0]["cities"], ["Макеевка"])
            self.assertEqual(list_drafts(db, category="fuel"), [])
            with self.assertRaises(ValueError):
                list_drafts(db, category="alerts")

    def test_missing_db_returns_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual(list_drafts(Path(tmp) / "missing.sqlite3"), [])

if __name__ == "__main__":
    unittest.main()
