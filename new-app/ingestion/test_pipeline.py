"""End-to-end offline test: Telegram HTML -> classification -> snapshot -> SQLite -> review API."""
import json
import sqlite3
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from telegram_fetch import TelegramPreviewParser, collect
from export_posts import export
from import_posts import import_drafts
from review_api import list_drafts

HTML = """
<div class="tgme_widget_message_wrap">
  <div class="tgme_widget_message">
    <div class="tgme_widget_message_text js-message_text">Подвоз воды <b>Макеевка</b> 29.10</div>
    <a class="tgme_widget_message_date" href="https://t.me/vodavdnr/42">
      <time datetime="2026-10-29T09:00:00+00:00">29 October</time>
    </a>
  </div>
</div>
"""

class PipelineTests(unittest.TestCase):
    def test_html_to_review_without_publication(self):
        parser = TelegramPreviewParser()
        parser.feed(HTML)
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            snapshot = root / "posts.json"
            db = root / "review.sqlite3"
            with patch("telegram_fetch.CHANNELS", {"voda_dnr": "vodavdnr"}), \
                 patch("telegram_fetch.fetch_channel", return_value=parser.posts):
                self.assertEqual(export(snapshot), 1)
            payload = json.loads(snapshot.read_text(encoding="utf-8"))
            self.assertEqual(payload["posts"][0]["category"], "water")
            self.assertEqual(payload["posts"][0]["kind"], "delivery")
            import_drafts(db, payload)
            import_drafts(db, payload)
            drafts = list_drafts(db, "water")
            self.assertEqual(len(drafts), 1)
            self.assertEqual(drafts[0]["source_url"], "https://t.me/vodavdnr/42")
            self.assertEqual(drafts[0]["moderation_status"], "draft")
            self.assertNotIn("cities_json", drafts[0])
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM review_posts").fetchone()[0], 1)

if __name__ == "__main__":
    unittest.main()
