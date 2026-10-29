import sqlite3
import tempfile
import unittest
from pathlib import Path
from import_posts import import_drafts

def payload():
    return {"schema_version": 1, "publication_status": "draft", "review_required": True,
            "posts": [{"source_id": "voda_dnr", "post_id": "42",
                       "source_url": "https://t.me/vodavdnr/42", "published_at": None,
                       "category": "water", "kind": "schedule", "cities": ["Макеевка"],
                       "text": "График воды", "requires_review": False}]}

class ImportTests(unittest.TestCase):
    def test_repeat_is_idempotent_and_remains_draft(self):
        with tempfile.TemporaryDirectory() as tmp:
            db = Path(tmp) / "review.sqlite"
            import_drafts(db, payload())
            import_drafts(db, payload())
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM review_posts").fetchone()[0], 1)
                self.assertEqual(conn.execute("SELECT moderation_status, requires_review FROM review_posts").fetchone(), ("draft", 1))

    def test_reject_wrong_source_url(self):
        p = payload()
        p["posts"][0]["source_url"] = "https://evil.example/42"
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                import_drafts(Path(tmp) / "db.sqlite", p)

    def test_reject_timestamp_without_timezone(self):
        p = payload()
        p["posts"][0]["published_at"] = "2026-10-29T09:00:00"
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                import_drafts(Path(tmp) / "db.sqlite", p)

    def test_existing_moderated_post_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            db = Path(tmp) / "db.sqlite"
            import_drafts(db, payload())
            with sqlite3.connect(db) as conn:
                conn.execute("UPDATE review_posts SET moderation_status='approved' WHERE post_id='42'")
            changed = payload()
            changed["posts"][0]["text"] = "Modified text"
            import_drafts(db, changed)
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT body FROM review_posts").fetchone()[0], "График воды")

    def test_reject_non_draft(self):
        p = payload()
        p["publication_status"] = "published"
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                import_drafts(Path(tmp) / "db.sqlite", p)

if __name__ == "__main__":
    unittest.main()
