"""Conservative classifier for public utility and municipal posts.

Classifications are suggestions only. No post is a live outage or public announcement
until a human moderator verifies the city, status, date, and source.
"""
from dataclasses import dataclass
import re

WATER = ("вод", "водоснабж", "водовоз", "подвоз")
POWER = ("электроэнерг", "электроснабж", "отключени", "свет", "электрич")
FUEL = ("бензин", "дизел", "топлив", "азс")

# Canonical city labels and common spellings. This is deliberately a text match,
# not an inference from which channel published the post.
CITY_ALIASES = {
    "Донецк": ("донецк",),
    "Макеевка": ("макеевк",),
    "Дебальцево": ("дебальцев",),
    "Докучаевск": ("докучаевск",),
    "Горловка": ("горловк",),
    "Енакиево": ("енак",),
    "Иловайск": ("иловайск",),
    "Мариуполь": ("мариупол",),
    "Снежное": ("снежн",),
    "Торез": ("торез", "чистяково"),
    "Харцызск": ("харцызск",),
    "Амвросиевка": ("амвросиев",),
    "Волноваха": ("волновах",),
    "Шахтёрск": ("шахтерск", "шахтёрск"),
    "Новоазовск": ("новоазовск",),
    "Тельманово": ("тельманов",),
    "Старобешево": ("старобешев",),
    "Володарское": ("володарск",),
    "Мангуш": ("мангуш",),
    "Ждановка": ("ждановк",),
    "Кировское": ("кировск", "крестовк"),
    "Ясиноватая": ("ясиноват",),
    "Красный Лиман": ("красн\u044bй лиман", "краснолиман"),
}

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
    normalized_aliases = {
        city: tuple(alias.replace("ё", "е") for alias in aliases)
        for city, aliases in CITY_ALIASES.items()
    }
    cities = tuple(city for city, aliases in normalized_aliases.items()
                   if any(alias in lower for alias in aliases))
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
    has_date = bool(re.search(r"\b\d{1,2}[./-]\d{1,2}(?:[./-]\d{2,4})?\b", lower))
    # General news, ambiguous city scope, and every municipal announcement require review.
    requires_review = not (len(cities) == 1 and has_date and category in ("water", "electricity"))
    return ParsedPost(category, cities, kind, requires_review, source_url, text)
