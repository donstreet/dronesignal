import unittest
from unittest.mock import patch
from telegram_fetch import TelegramPreviewParser, collect

HTML = """
<div class="tgme_widget_message_wrap">
 <div class="tgme_widget_message">
  <div class="tgme_widget_message_text js-message_text">График <b>отключения</b> света Макеевка 29.10</div>
  <a class="tgme_widget_message_date" href="https://t.me/Novosti_Makeevka/42"><time datetime="2026-10-29T07:00:00+00:00">date</time></a>
 </div>
</div>
"""

class PreviewTests(unittest.TestCase):
    def test_parse_nested_markup(self):
        p = TelegramPreviewParser()
        p.feed(HTML)
        self.assertEqual(len(p.posts), 1)
        self.assertIn("отключения", p.posts[0]["text"])
        self.assertEqual(p.posts[0]["url"], "https://t.me/Novosti_Makeevka/42")

    def test_nested_markup_keeps_word_boundaries(self):
        p = TelegramPreviewParser()
        p.feed("""<div class="tgme_widget_message_wrap"><div>
          <div class="tgme_widget_message_text">Подвоз <b>воды</b> Макеевка 29.10</div>
          <a class="tgme_widget_message_date" href="https://t.me/vodavdnr/42">
          <time datetime="2026-10-29T09:00:00+00:00">date</time></a>
          </div></div>""")
        self.assertEqual(len(p.posts), 1)
        self.assertIn("Подвоз воды Макеевка", p.posts[0]["text"])

    def test_dedup_and_source(self):
        with patch("telegram_fetch.CHANNELS", {"makeevka_news": "Novosti_Makeevka"}), \
             patch("telegram_fetch.fetch_channel", return_value=[
                 {"url": "https://t.me/Novosti_Makeevka/42", "text": "График света Макеевка 29.10", "published_at": "2026-10-29"},
                 {"url": "https://t.me/Novosti_Makeevka/42", "text": "График света Макеевка 29.10", "published_at": "2026-10-29"}
             ]):
            data = collect()
        self.assertEqual(len(data["posts"]), 1)
        self.assertEqual(data["posts"][0]["source_id"], "makeevka_news")

if __name__ == "__main__":
    unittest.main()
