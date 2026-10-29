"""Export fetched Telegram posts into moderation staging, never live publication.

Partial success is allowed when at least one source produced posts; failed sources are
recorded in the snapshot. If all sources fail or no posts are available, keep the previous
snapshot unchanged.
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
    posts = result.get("posts", [])
    errors = result.get("errors", {})
    if errors and not posts:
        raise RuntimeError("Source fetch failed: " + ", ".join(sorted(errors)))
    if not posts:
        raise RuntimeError("No posts fetched; retaining last successful snapshot")
    payload = {
        "schema_version": 1,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "review_required": True,
        "publication_status": "draft",
        "source_errors": errors,
        "partial_source_failure": bool(errors),
        "posts": posts,
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
    return len(posts)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    print(f"Exported {export(args.output)} posts for review")
