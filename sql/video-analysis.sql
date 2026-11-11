CREATE TABLE IF NOT EXISTS video_analysis (
    video_id VARCHAR(64) PRIMARY KEY,
    version BIGINT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    concern VARCHAR(1200) NOT NULL,
    evidence VARCHAR(10000) NOT NULL,
    draft VARCHAR(240) NOT NULL,
    attempt VARCHAR(36) NOT NULL
);

CREATE TABLE IF NOT EXISTS analysis_revision (
    attempt VARCHAR(36) PRIMARY KEY,
    video_id VARCHAR(64) NOT NULL REFERENCES video_analysis(video_id),
    recorded_at TIMESTAMP WITH TIME ZONE,
    outcome VARCHAR(32) NOT NULL,
    evidence VARCHAR(10000) NOT NULL,
    draft VARCHAR(240) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_analysis_revision_video ON analysis_revision(video_id, recorded_at);

-- Legacy snapshots have no generation timestamp.
INSERT INTO analysis_revision (attempt, video_id, outcome, evidence, draft)
SELECT a.attempt, a.video_id, a.stage, a.evidence, CASE WHEN a.stage = 'READY' THEN a.draft ELSE '' END
FROM video_analysis a
WHERE a.stage IN ('READY','FAILED','INSUFFICIENT')
  AND NOT EXISTS (SELECT 1 FROM analysis_revision r WHERE r.attempt = a.attempt);

ALTER TABLE video_analysis ADD COLUMN IF NOT EXISTS started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE video_analysis ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(1000);
ALTER TABLE video_analysis ADD COLUMN IF NOT EXISTS analysis_type VARCHAR(32);
ALTER TABLE analysis_revision ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(1000);
ALTER TABLE analysis_revision ADD COLUMN IF NOT EXISTS screenshot TEXT;
