"""Conservative parsing of utility schedules from user-approved channel posts.

This module does not fetch Telegram/MAX or declare a schedule currently active.
"""
from dataclasses import dataclass
import re

WATER = ("вод", "водоснабж", "водовоз", "подвоз")
POWER = ("электроэнерг", "электроснабж", "отключени", "света", "электрич")
FUEL = ("бензин", "дизел", "топлив", "азс")
CITIES = ("Макеевка", "Донецк")

@dataclass(frozen=True)
class ParsedPost:
    category: str
    cities: tuple[str, ...]
    kind: str
    requires_review: bool
    source_url: str
    text: str

def parse_post(text: str, source_url: str) -> ParsedPost:
    lower = text.casefold().replace("ё", "е")
    water = any(x in lower for x in WATER)
    power = any(x in lower for x in POWER)
    fuel = any(x in lower for x in FUEL)
    if fuel and not (water or power):
        category = "fuel"
    elif water and not power:
        category = "water"
    elif power and not water:
        category = "electricity"
    elif water and power:
        category = "mixed_utilities"
    else:
        category = "news"
    cities = tuple(c for c in CITIES if c.casefold() in lower)
    if "подвоз" in lower or "водовоз" in lower:
        kind = "delivery"
    elif "восстанов" in lower or "возобновлен" in lower:
        kind = "restored"
    elif "график" in lower or "расписани" in lower:
        kind = "schedule"
    elif "отключ" in lower or "ограничени" in lower:
        kind = "outage_notice"
    else:
        kind = "general"
    # No automatic 'active outage' without unambiguous city, date and source.
    has_date = bool(re.search(r"\b\d{1,2}[./-]\d{1,2}(?:[./-]\d{2,4})?\b", lower))
    requires_review = not (len(cities) == 1 and has_date and category in ("water", "electricity"))
    return ParsedPost(category, cities, kind, requires_review, source_url, text)
