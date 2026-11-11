CREATE TABLE IF NOT EXISTS video_analysis (
    video_id VARCHAR(64) PRIMARY KEY,
    version BIGINT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    concern VARCHAR(1200) NOT NULL,
    evidence VARCHAR(10000) NOT NULL,
    draft VARCHAR(240) NOT NULL,
    attempt VARCHAR(36) NOT NULL
);
