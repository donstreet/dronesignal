import sqlite3
import tempfile
import unittest
from pathlib import Path
from pipeline import run

POST = {"source_id": "voda_dnr", "post_id": "42", "source_url": "https://t.me/vodavdnr/42",
        "published_at": "2026-10-29T09:00:00+00:00", "category": "water", "kind": "delivery",
        "cities": ["Макеевка"], "text": "Подвоз воды Макеевка 29.10", "requires_review": False}

class PipelineCommandTests(unittest.TestCase):
    def test_repeated_run_stays_one_draft(self):
        with tempfile.TemporaryDirectory() as tmp:
            db, snapshot = Path(tmp) / "db.sqlite", Path(tmp) / "posts.json"
            collector = lambda: {"posts": [POST], "errors": {}}
            self.assertEqual(run(db, snapshot, collector)["published"], 0)
            self.assertEqual(run(db, snapshot, collector)["notifications_sent"], 0)
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM review_posts").fetchone()[0], 1)
                self.assertEqual(conn.execute("SELECT moderation_status FROM review_posts").fetchone()[0], "draft")

    def test_source_failure_does_not_change_db_or_snapshot(self):
        with tempfile.TemporaryDirectory() as tmp:
            db, snapshot = Path(tmp) / "db.sqlite", Path(tmp) / "posts.json"
            run(db, snapshot, lambda: {"posts": [POST], "errors": {}})
            original = snapshot.read_bytes()
            with self.assertRaises(RuntimeError):
                run(db, snapshot, lambda: {"posts": [], "errors": {"voda_dnr": "timeout"}})
            self.assertEqual(snapshot.read_bytes(), original)
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM review_posts").fetchone()[0], 1)

if __name__ == "__main__":
    unittest.main()
