import sqlite3
import tempfile
import unittest
from pathlib import Path

from import_posts import import_drafts
from publish_news import publish_news
from public_outages_api import get_news, initialize

def payload():
    return {"schema_version": 1, "publication_status": "draft", "review_required": True,
            "posts": [{"source_id": "gorlovka_admin", "post_id": "42",
                       "source_url": "https://t.me/Gorlowka_now/42",
                       "published_at": "2026-10-29T12:00:00+00:00",
                       "category": "news", "kind": "general", "cities": ["Горловка"],
                       "text": "Администрация Горловки сообщает о работах",
                       "requires_review": True}]}

class PublishNewsTests(unittest.TestCase):
    def test_requires_explicit_confirmation_and_publishes_verified_draft(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            review_db, public_db = root / "review.sqlite3", root / "public.sqlite3"
            import_drafts(review_db, payload())
            with self.assertRaises(ValueError):
                publish_news(review_db, public_db, "gorlovka_admin", "42", "Горловка",
                             "Работы на улице", "Проверенное сообщение", False)
            news_id = publish_news(review_db, public_db, "gorlovka_admin", "42", "Горловка",
                                   "Работы на улице", "Проверенное сообщение", True)
            self.assertEqual(news_id, "gorlovka_admin:42")
            rows = get_news(public_db, "Горловка")
            self.assertEqual(len(rows), 1)
            self.assertEqual(rows[0]["source_url"], "https://t.me/Gorlowka_now/42")
            self.assertEqual(rows[0]["verification"], "confirmed")
            with sqlite3.connect(review_db) as db:
                status = db.execute("SELECT moderation_status FROM review_posts WHERE post_id='42'").fetchone()[0]
            self.assertEqual(status, "published")
            with self.assertRaises(ValueError):
                publish_news(review_db, public_db, "gorlovka_admin", "42", "Горловка",
                             "Другой заголовок", "Текст", True)

    def test_wrong_city_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            review_db, public_db = root / "review.sqlite3", root / "public.sqlite3"
            import_drafts(review_db, payload())
            with self.assertRaises(ValueError):
                publish_news(review_db, public_db, "gorlovka_admin", "42", "Несуществующий город",
                             "Заголовок", "Текст", True)

if __name__ == "__main__":
    unittest.main()
