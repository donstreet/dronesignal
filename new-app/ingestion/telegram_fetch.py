"""Fetch public Telegram channel previews; never infer official affiliation or live outage.

Usage: python3 telegram_fetch.py > posts.json
Only user-approved public Telegram channels are read. MAX requires a separate approved API.
"""
import json
import re
import urllib.request
from html.parser import HTMLParser
from dataclasses import asdict
from classifier import parse_post

CHANNELS = {
    "makeevka_news": "Novosti_Makeevka",
    "fuel_dnr": "fueldnr",
    "benz_dnr": "benz_dnr",
    "voda_dnr": "vodavdnr",
}

class TelegramPreviewParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.depth = 0
        self.post = None
        self.text_depth = None
        self.posts = []

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        classes = a.get("class", "").split()
        if self.post is not None:
            self.depth += 1
            if "tgme_widget_message_text" in classes:
                self.text_depth = self.depth
            if tag == "a" and "tgme_widget_message_date" in classes:
                self.post["url"] = a.get("href", self.post["url"])
            if tag == "time":
                self.post["published_at"] = a.get("datetime")
        elif "tgme_widget_message_wrap" in classes:
            self.post = {"url": "", "published_at": None, "text": ""}
            self.depth = 1

    def handle_data(self, data):
        if self.post is not None and self.text_depth is not None:
            self.post["text"] += data

    def handle_endtag(self, tag):
        if self.post is None:
            return
        if self.text_depth == self.depth:
            self.text_depth = None
        self.depth -= 1
        if self.depth == 0:
            if self.post["url"] and self.post["text"].strip():
                self.post["text"] = re.sub(r"\s+", " ", self.post["text"]).strip()
                self.posts.append(self.post)
            self.post = None

def fetch_channel(channel):
    url = "https://t.me/s/" + channel
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (compatible; MoiGorod/0.1)"})
    with urllib.request.urlopen(req, timeout=15) as response:
        html = response.read(2_000_000).decode("utf-8", errors="replace")
    parser = TelegramPreviewParser()
    parser.feed(html)
    return parser.posts

def collect():
    output, errors, seen = [], {}, set()
    for source_id, channel in CHANNELS.items():
        try:
            for post in fetch_channel(channel):
                match = re.fullmatch(r"https://t\.me/" + re.escape(channel) + r"/(\d+)", post["url"])
                if not match:
                    continue
                key = (source_id, match.group(1))
                if key in seen:
                    continue
                seen.add(key)
                parsed = parse_post(post["text"], post["url"])
                output.append({"source_id": source_id, "post_id": match.group(1),
                               "published_at": post["published_at"], **asdict(parsed)})
        except (OSError, ValueError) as exc:
            errors[source_id] = str(exc)
    return {"posts": output, "errors": errors, "status": "unverified_preview_not_live_schedule"}

if __name__ == "__main__":
    print(json.dumps(collect(), ensure_ascii=False, indent=2))
