"""Moy Gorod announcements router for deployed FastAPI + psycopg 3.

Copy alongside main.py and register:
    from announcements_psycopg import router
    app.include_router(router)

Requires migration 001_announcements.sql and ADMIN_API_KEY in API environment.
Admin key is server-only; do not embed it in a distributable APK.
"""
import hmac
import os
from datetime import datetime
from typing import Literal

import psycopg
from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field, HttpUrl, model_validator
from psycopg.rows import dict_row

router = APIRouter(prefix="/api", tags=["announcements"])
Category = Literal["general", "electricity", "water", "news", "emergency"]

def connection():
    return psycopg.connect(
        host=os.getenv("DB_HOST", "postgres"),
        dbname=os.getenv("POSTGRES_DB", "dronesignal"),
        user=os.getenv("POSTGRES_USER", "dronesignal"),
        password=os.environ["POSTGRES_PASSWORD"],
        connect_timeout=3, row_factory=dict_row,
    )

def require_admin(key: str | None):
    expected = os.getenv("ADMIN_API_KEY")
    if not expected or not key or not hmac.compare_digest(key, expected):
        raise HTTPException(status_code=401, detail="Unauthorized")

class Announcement(BaseModel):
    title: str = Field(min_length=1, max_length=180)
    body: str = Field(min_length=1, max_length=10000)
    category: Category
    image_url: HttpUrl | None = None
    action_url: HttpUrl | None = None
    action_phone: str | None = Field(default=None, pattern=r"^\+?[0-9 ()-]{5,32}$")
    publish_at: datetime | None = None
    expires_at: datetime | None = None

    @model_validator(mode="after")
    def validate_fields(self):
        if not self.title.strip() or not self.body.strip():
            raise ValueError("Title and body must not be blank")
        if self.publish_at and self.expires_at and self.expires_at <= self.publish_at:
            raise ValueError("Expiry must follow publication")
        return self

COLUMNS = """id,title,body,category,image_url,action_url,action_phone,status,
publish_at,expires_at,published_at,created_at,updated_at"""

@router.get("/announcements")
def feed():
    with connection() as conn:
        rows = conn.execute(f"""SELECT {COLUMNS} FROM city_announcements
            WHERE status='published' AND (publish_at IS NULL OR publish_at <= now())
            AND (expires_at IS NULL OR expires_at > now())
            ORDER BY published_at DESC NULLS LAST, id DESC LIMIT 100""").fetchall()
    return {"announcements": rows}

@router.get("/admin/announcements")
def list_admin(x_admin_api_key: str | None = Header(default=None)):
    require_admin(x_admin_api_key)
    with connection() as conn:
        rows = conn.execute(f"SELECT {COLUMNS} FROM city_announcements ORDER BY id DESC LIMIT 200").fetchall()
    return {"announcements": rows}

@router.post("/admin/announcements", status_code=201)
def create(data: Announcement, x_admin_api_key: str | None = Header(default=None)):
    require_admin(x_admin_api_key)
    with connection() as conn:
        row = conn.execute(f"""INSERT INTO city_announcements
        (title,body,category,image_url,action_url,action_phone,publish_at,expires_at,created_by,updated_by)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING {COLUMNS}""",
        (data.title.strip(),data.body.strip(),data.category,
        str(data.image_url) if data.image_url else None,
        str(data.action_url) if data.action_url else None,
        data.action_phone,data.publish_at,data.expires_at,"admin","admin")).fetchone()
        conn.execute("""INSERT INTO city_announcement_audit(announcement_id,actor,action)
        VALUES (%s,%s,'create')""",(row["id"],"admin"))
    return row

@router.patch("/admin/announcements/{announcement_id}")
def edit(announcement_id: int, data: Announcement, x_admin_api_key: str | None = Header(default=None)):
    require_admin(x_admin_api_key)
    with connection() as conn:
        row = conn.execute(f"""UPDATE city_announcements SET title=%s,body=%s,category=%s,
        image_url=%s,action_url=%s,action_phone=%s,publish_at=%s,expires_at=%s,
        updated_at=now(),updated_by='admin' WHERE id=%s AND status!='published'
        RETURNING {COLUMNS}""",
        (data.title.strip(),data.body.strip(),data.category,
        str(data.image_url) if data.image_url else None,
        str(data.action_url) if data.action_url else None,
        data.action_phone,data.publish_at,data.expires_at,announcement_id)).fetchone()
        if row is None: raise HTTPException(404,"Draft/unpublished announcement not found")
        conn.execute("""INSERT INTO city_announcement_audit(announcement_id,actor,action)
        VALUES (%s,%s,'edit')""",(announcement_id,"admin"))
    return row

@router.post("/admin/announcements/{announcement_id}/publish")
def publish(announcement_id: int, x_admin_api_key: str | None = Header(default=None)):
    require_admin(x_admin_api_key)
    with connection() as conn:
        row = conn.execute(f"""UPDATE city_announcements SET status='published',
        published_at=now(),publish_at=COALESCE(publish_at,now()),
        updated_at=now(),updated_by='admin' WHERE id=%s
        AND status IN ('draft','unpublished')
        AND (expires_at IS NULL OR expires_at > now()) RETURNING {COLUMNS}""",
        (announcement_id,)).fetchone()
        if row is None: raise HTTPException(409,"Not publishable")
        conn.execute("""INSERT INTO city_announcement_audit(announcement_id,actor,action)
        VALUES (%s,%s,'publish')""",(announcement_id,"admin"))
    return row

@router.post("/admin/announcements/{announcement_id}/unpublish")
def unpublish(announcement_id: int, x_admin_api_key: str | None = Header(default=None)):
    require_admin(x_admin_api_key)
    with connection() as conn:
        row = conn.execute(f"""UPDATE city_announcements SET status='unpublished',
        updated_at=now(),updated_by='admin' WHERE id=%s AND status='published'
        RETURNING {COLUMNS}""",(announcement_id,)).fetchone()
        if row is None: raise HTTPException(409,"Not currently published")
        conn.execute("""INSERT INTO city_announcement_audit(announcement_id,actor,action)
        VALUES (%s,%s,'unpublish')""",(announcement_id,"admin"))
    return row

@router.post("/admin/announcements/{announcement_id}/notify", status_code=202)
def notify(announcement_id: int, x_admin_api_key: str | None = Header(default=None)):
    require_admin(x_admin_api_key)
    with connection() as conn:
        row = conn.execute("""SELECT id FROM city_announcements WHERE id=%s
          AND status='published' AND (publish_at IS NULL OR publish_at<=now())
          AND (expires_at IS NULL OR expires_at>now()) FOR UPDATE""",(announcement_id,)).fetchone()
        if row is None: raise HTTPException(409,"Announcement not active")
        inserted = conn.execute("""INSERT INTO city_announcement_notifications(announcement_id,requested_by)
          VALUES (%s,%s) ON CONFLICT DO NOTHING RETURNING announcement_id""",
          (announcement_id,"admin")).fetchone()
        if inserted is None: raise HTTPException(409,"Already queued")
        conn.execute("""INSERT INTO city_announcement_audit(announcement_id,actor,action)
          VALUES (%s,%s,'notify_queued')""",(announcement_id,"admin"))
    return {"status":"queued","announcement_id":announcement_id}
