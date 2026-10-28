"""FastAPI router for city announcements.

Integration contract: mount this router into the existing API with a PostgreSQL
connection pool at app.state.db_pool and authenticated principal at request.state.user.
The existing auth middleware MUST validate the token server-side and set a dict
with 'id' and 'role'. Never trust client-provided role headers.
"""
from datetime import datetime, timezone
from typing import Literal
from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, Field, HttpUrl, model_validator

router = APIRouter(prefix="/api", tags=["announcements"])
Category = Literal["general", "electricity", "water", "news", "emergency"]

class AnnouncementInput(BaseModel):
    title: str = Field(min_length=1, max_length=180)
    body: str = Field(min_length=1, max_length=10000)
    category: Category
    image_url: HttpUrl | None = None
    action_url: HttpUrl | None = None
    action_phone: str | None = Field(default=None, pattern=r"^\\+?[0-9 ()-]{5,32}$")
    publish_at: datetime | None = None
    expires_at: datetime | None = None

    @model_validator(mode="after")
    def validate_dates(self):
        if not self.title.strip() or not self.body.strip():
            raise ValueError("Title and body cannot be blank")
        if self.publish_at and self.expires_at and self.expires_at <= self.publish_at:
            raise ValueError("Expiry must be after publication")
        return self

async def pool(request: Request):
    value = getattr(request.app.state, "db_pool", None)
    if value is None:
        raise HTTPException(503, "Database unavailable")
    return value

async def admin(request: Request):
    principal = getattr(request.state, "user", None)
    if not isinstance(principal, dict) or principal.get("role") != "admin" or not principal.get("id"):
        raise HTTPException(403, "Administrator authorization required")
    return str(principal["id"])

COLUMNS = """id,title,body,category,image_url,action_url,action_phone,status,
publish_at,expires_at,published_at,created_at,updated_at"""

@router.get("/announcements")
async def public_feed(db=Depends(pool)):
    now = datetime.now(timezone.utc)
    async with db.acquire() as conn:
        rows = await conn.fetch(f"""SELECT {COLUMNS} FROM city_announcements
          WHERE status='published' AND (publish_at IS NULL OR publish_at <= $1)
          AND (expires_at IS NULL OR expires_at > $1)
          ORDER BY published_at DESC NULLS LAST,id DESC LIMIT 100""", now)
    return [dict(row) for row in rows]

@router.get("/admin/announcements")
async def admin_feed(actor=Depends(admin), db=Depends(pool)):
    async with db.acquire() as conn:
        rows = await conn.fetch(f"SELECT {COLUMNS} FROM city_announcements ORDER BY id DESC LIMIT 200")
    return [dict(row) for row in rows]

@router.post("/admin/announcements", status_code=201)
async def create_announcement(data: AnnouncementInput, actor=Depends(admin), db=Depends(pool)):
    async with db.acquire() as conn, conn.transaction():
        row = await conn.fetchrow(f"""INSERT INTO city_announcements
        (title,body,category,image_url,action_url,action_phone,publish_at,expires_at,created_by,updated_by)
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$9) RETURNING {COLUMNS}""",
        data.title.strip(),data.body.strip(),data.category,
        str(data.image_url) if data.image_url else None,
        str(data.action_url) if data.action_url else None,
        data.action_phone,data.publish_at,data.expires_at,actor)
        await conn.execute("""INSERT INTO city_announcement_audit(announcement_id,actor,action)
        VALUES($1,$2,'create')""",row["id"],actor)
    return dict(row)

@router.patch("/admin/announcements/{announcement_id}")
async def edit_announcement(announcement_id: int, data: AnnouncementInput, actor=Depends(admin), db=Depends(pool)):
    async with db.acquire() as conn, conn.transaction():
        row = await conn.fetchrow(f"""UPDATE city_announcements SET title=$2,body=$3,category=$4,
        image_url=$5,action_url=$6,action_phone=$7,publish_at=$8,expires_at=$9,
        updated_by=$10,updated_at=now() WHERE id=$1 AND status!='published' RETURNING {COLUMNS}""",
        announcement_id,data.title.strip(),data.body.strip(),data.category,
        str(data.image_url) if data.image_url else None,
        str(data.action_url) if data.action_url else None,
        data.action_phone,data.publish_at,data.expires_at,actor)
        if row is None: raise HTTPException(404,"Draft/unpublished announcement not found")
        await conn.execute("INSERT INTO city_announcement_audit(announcement_id,actor,action) VALUES($1,$2,'edit')",announcement_id,actor)
    return dict(row)

@router.post("/admin/announcements/{announcement_id}/publish")
async def publish(announcement_id: int, actor=Depends(admin), db=Depends(pool)):
    async with db.acquire() as conn, conn.transaction():
        row = await conn.fetchrow(f"""UPDATE city_announcements SET status='published',
        published_at=now(),publish_at=COALESCE(publish_at,now()),updated_by=$2,updated_at=now()
        WHERE id=$1 AND status IN ('draft','unpublished')
        AND (expires_at IS NULL OR expires_at > now())
        RETURNING {COLUMNS}""",announcement_id,actor)
        if row is None: raise HTTPException(409,"Not publishable")
        await conn.execute("INSERT INTO city_announcement_audit(announcement_id,actor,action) VALUES($1,$2,'publish')",announcement_id,actor)
    return dict(row)

@router.post("/admin/announcements/{announcement_id}/unpublish")
async def unpublish(announcement_id: int, actor=Depends(admin), db=Depends(pool)):
    async with db.acquire() as conn, conn.transaction():
        row = await conn.fetchrow(f"""UPDATE city_announcements SET status='unpublished',
        updated_by=$2,updated_at=now() WHERE id=$1 AND status='published' RETURNING {COLUMNS}""",announcement_id,actor)
        if row is None: raise HTTPException(409,"Not currently published")
        await conn.execute("INSERT INTO city_announcement_audit(announcement_id,actor,action) VALUES($1,$2,'unpublish')",announcement_id,actor)
    return dict(row)

@router.post("/admin/announcements/{announcement_id}/notify", status_code=202)
async def queue_notification(announcement_id: int, actor=Depends(admin), db=Depends(pool)):
    async with db.acquire() as conn, conn.transaction():
        row = await conn.fetchrow("""SELECT id FROM city_announcements WHERE id=$1 AND status='published'
          AND (publish_at IS NULL OR publish_at <= now())
          AND (expires_at IS NULL OR expires_at > now()) FOR UPDATE""",announcement_id)
        if row is None: raise HTTPException(409,"No active published announcement")
        result = await conn.fetchrow("""INSERT INTO city_announcement_notifications(announcement_id,requested_by)
          VALUES($1,$2) ON CONFLICT DO NOTHING RETURNING announcement_id""",announcement_id,actor)
        if result is None: raise HTTPException(409,"Notification already requested")
        await conn.execute("INSERT INTO city_announcement_audit(announcement_id,actor,action) VALUES($1,$2,'notify_queued')",announcement_id,actor)
    return {"status":"queued","announcement_id":announcement_id}
