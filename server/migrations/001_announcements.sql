-- Run against the existing PostgreSQL database after backup and schema review.
CREATE TABLE IF NOT EXISTS city_announcements (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 title VARCHAR(180) NOT NULL CHECK (length(trim(title)) > 0),
 body TEXT NOT NULL CHECK (length(trim(body)) > 0 AND length(body) <= 10000),
 category VARCHAR(24) NOT NULL CHECK (category IN ('general','electricity','water','news','emergency')),
 image_url TEXT,
 action_url TEXT,
 action_phone VARCHAR(32),
 status VARCHAR(16) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','published','unpublished')),
 publish_at TIMESTAMPTZ,
 expires_at TIMESTAMPTZ,
 published_at TIMESTAMPTZ,
 created_by TEXT NOT NULL,
 updated_by TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (expires_at IS NULL OR publish_at IS NULL OR expires_at > publish_at)
);
CREATE INDEX IF NOT EXISTS idx_city_announcements_feed ON city_announcements (status, publish_at DESC, id DESC);
CREATE TABLE IF NOT EXISTS city_announcement_audit (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 announcement_id BIGINT NOT NULL REFERENCES city_announcements(id),
 actor TEXT NOT NULL,
 action VARCHAR(32) NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS city_announcement_notifications (
 announcement_id BIGINT PRIMARY KEY REFERENCES city_announcements(id),
 requested_by TEXT NOT NULL,
 requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 state VARCHAR(16) NOT NULL DEFAULT 'pending' CHECK (state IN ('pending','sent','failed')),
 sent_at TIMESTAMPTZ
);
