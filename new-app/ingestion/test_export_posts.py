import json
import tempfile
import unittest
from pathlib import Path
from export_posts import export

class ExportTests(unittest.TestCase):
    def test_successful_export_is_draft(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "posts.json"
            count = export(target, lambda: {"posts": [{"source_id": "voda_dnr", "post_id": "1"}], "errors": {}})
            self.assertEqual(count, 1)
            result = json.loads(target.read_text(encoding="utf-8"))
            self.assertTrue(result["review_required"])
            self.assertEqual(result["publication_status"], "draft")

    def test_fetch_failure_preserves_previous_snapshot(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "posts.json"
            target.write_text("previous", encoding="utf-8")
            with self.assertRaises(RuntimeError):
                export(target, lambda: {"posts": [], "errors": {"voda_dnr": "timeout"}})
            self.assertEqual(target.read_text(encoding="utf-8"), "previous")

    def test_partial_fetch_saves_posts_and_source_errors(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "posts.json"
            result = {"posts": [{"source_id": "donetsk_admin", "post_id": "7"}],
                      "errors": {"mariupol_admin": "timeout"}}
            self.assertEqual(export(target, lambda: result), 1)
            payload = json.loads(target.read_text(encoding="utf-8"))
            self.assertTrue(payload["partial_source_failure"])
            self.assertEqual(payload["source_errors"]["mariupol_admin"], "timeout")
            self.assertEqual(payload["publication_status"], "draft")

    def test_empty_fetch_does_not_erase_snapshot(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "posts.json"
            target.write_text("previous", encoding="utf-8")
            with self.assertRaises(RuntimeError):
                export(target, lambda: {"posts": [], "errors": {}})
            self.assertEqual(target.read_text(encoding="utf-8"), "previous")

if __name__ == "__main__":
    unittest.main()
