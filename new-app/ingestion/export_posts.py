"""Persist fetched public posts for moderation/API import, without publishing them as live outages.

Usage: python3 export_posts.py --output /path/to/review-posts.json
Exit nonzero on source errors; preserve the previous snapshot on failure.
"""
import argparse
import json
import os
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from telegram_fetch import collect

def export(output: Path, collector=collect):
    result = collector()
    if result["errors"]:
        raise RuntimeError("Source fetch failed: " + ", ".join(sorted(result["errors"])))
    if not result["posts"]:
        raise RuntimeError("No posts fetched; retaining last successful snapshot")
    payload = {
        "schema_version": 1,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "review_required": True,
        "publication_status": "draft",
        "posts": result["posts"],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    name = None
    try:
        with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=output.parent,
                                         prefix=".posts-", suffix=".tmp", delete=False) as f:
            name = f.name
            json.dump(payload, f, ensure_ascii=False, indent=2)
            f.write("\n")
            f.flush()
            os.fsync(f.fileno())
        os.replace(name, output)
    finally:
        if name and os.path.exists(name):
            os.unlink(name)
    return len(result["posts"])

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    print(f"Exported {export(args.output)} posts for review")
