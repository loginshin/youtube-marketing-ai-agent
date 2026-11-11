ALTER TABLE comment_job ADD COLUMN IF NOT EXISTS pending_comment_id VARCHAR(256);
CREATE TABLE IF NOT EXISTS posted_comment (
    comment_id VARCHAR(256) PRIMARY KEY,
    job_id BIGINT NOT NULL UNIQUE REFERENCES comment_job(id),
    video_id VARCHAR(64) NOT NULL UNIQUE,
    video_title VARCHAR(512) NOT NULL,
    video_url VARCHAR(512) NOT NULL,
    comment_url VARCHAR(1024) NOT NULL,
    profile_name VARCHAR(128) NOT NULL,
    author_name VARCHAR(256) NOT NULL,
    author_channel_id VARCHAR(128) NOT NULL,
    body VARCHAR(240) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_posted_comment_time ON posted_comment(published_at);
