"""Fetch public Telegram channel previews into a moderation-only staging pipeline.

Only explicitly allowlisted public Telegram channels are fetched. MAX is not fetched:
a public MAX URL is not an API and does not authorize automated collection.
Usage: python3 telegram_fetch.py > posts.json
"""
import json
import re
import urllib.request
from datetime import datetime
from html.parser import HTMLParser
from dataclasses import asdict
from classifier import parse_post

# Keys are stable internal IDs; values are exact public Telegram channel usernames.
# Municipal channels are used as sources, not as proof that every post concerns that city.
CHANNELS = {
    "makeevka_news": "Novosti_Makeevka",
    "fuel_dnr": "fueldnr",
    "benz_dnr": "benz_dnr",
    "voda_dnr": "vodavdnr",
    "donetsk_admin": "gorodDonetsk",
    "donetsk_head": "kulemzin_donetsk",
    "debaltsevo_admin": "admDebaltsevo",
    "debaltsevo_head": "glavadebaltsevo",
    "dokuchaevsk_admin": "Dokuchaevsk_admin",
    "dokuchaevsk_head": "dokuchaevsk",
    "gorlovka_admin": "Gorlowka_now",
    "enakievo_admin": "Enakievo_official",
    "enakievo_head": "IgorMartynov_enk",
    "ilovaisk_admin": "ilovaisk_onlain",
    "ilovaisk_head": "dz_ilov_gorokrug",
    "mariupol_admin": "nmrpl",
    "mariupol_head": "morgun_ov",
    "snezhnoe_admin": "Snijne",
    "snezhnoe_head": "Ermakov_ESV",
    "torez_admin": "torez_admin",
    "khartsyzsk_admin": "khartsyzsk_press_office",
    "amvrosievka_admin": "amvr_admin",
    "amvrosievka_head": "Lyzov_amvrosievka",
    "volnovakha_admin": "volnovahadnr",
    "volnovakha_head": "kazinchenko",
    "shakhtarsk_admin": "AGShahtersk",
    "shakhtarsk_head": "shatov_av_agsh",
    "novoazovsk_admin": "novoazovsk_adm",
    "novoazovsk_head": "novoazovskiy_region",
    "telmanovo_admin": "adm_telmanovskogo_rajona",
    "starobeshevo_admin": "starobeshevo1923",
    "volodarsk_admin": "volodinfo",
    "mangush_admin": "Mangush_mo",
    "zhdanovka_admin": "adm_zhdanovka",
    "kirovskoe_admin": "gorod_kirovskoe",
    "yasinovataya_admin": "gromko_yas",
    "krasny_liman_admin": "kr_liman_admin",
}

class TelegramPreviewParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.depth = 0
        self.post = None
        self.text_depth = None
        self.text_parts = []
        self.text_block_depth = None
        self.posts = []

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        classes = a.get("class", "").split()
        if self.post is not None:
            if tag == "div":
                self.depth += 1
            if tag == "div" and "tgme_widget_message_text" in classes:
                self.text_depth = self.depth
                self.text_parts = []
                self.text_block_depth = self.depth
            if tag == "a" and "tgme_widget_message_date" in classes:
                self.post["url"] = a.get("href", self.post["url"])
            if tag == "time":
                self.post["published_at"] = a.get("datetime")
        elif tag == "div" and "tgme_widget_message_wrap" in classes:
            self.post = {"url": "", "published_at": None, "text": ""}
            self.depth = 1

    def handle_data(self, data):
        if self.post is not None and self.text_depth is not None:
            self.text_parts.append(data)

    def handle_endtag(self, tag):
        if self.post is None or tag != "div":
            return
        if self.text_block_depth == self.depth:
            self.post["text"] += " ".join(self.text_parts)
            self.text_parts = []
            self.text_depth = None
            self.text_block_depth = None
        self.depth -= 1
        if self.depth == 0:
            if self.post["url"] and self.post["text"].strip():
                self.post["text"] = re.sub(r"\\s+", " ", self.post["text"]).strip()
                self.posts.append(self.post)
            self.post = None

def fetch_channel(channel):
    url = "https://t.me/s/" + channel
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (compatible; MoiGorod/0.1)"})
    with urllib.request.urlopen(req, timeout=12) as response:
        html = response.read(2_000_000).decode("utf-8", errors="replace")
    parser = TelegramPreviewParser()
    parser.feed(html)
    return parser.posts

def collect():
    output, errors, seen = [], {}, set()
    for source_id, channel in CHANNELS.items():
        try:
            for post in fetch_channel(channel):
                match = re.fullmatch(r"https://t\\.me/" + re.escape(channel) + r"/(\\d+)", post["url"])
                if not match:
                    continue
                key = (source_id, match.group(1))
                if key in seen:
                    continue
                seen.add(key)
                published_at = post.get("published_at")
                if not published_at:
                    continue
                try:
                    published = datetime.fromisoformat(published_at)
                    if published.tzinfo is None or published.utcoffset() is None:
                        continue
                except (TypeError, ValueError):
                    continue
                parsed = parse_post(post["text"], post["url"])
                output.append({"source_id": source_id, "post_id": match.group(1),
                               "published_at": post["published_at"], **asdict(parsed)})
        except (OSError, ValueError) as exc:
            errors[source_id] = str(exc)[:300]
    return {"posts": output, "errors": errors,
            "status": "partial_preview_requires_moderation" if output and errors else
                      "preview_requires_moderation" if output else "unverified_preview_not_live_schedule"}

if __name__ == "__main__":
    print(json.dumps(collect(), ensure_ascii=False, indent=2))
