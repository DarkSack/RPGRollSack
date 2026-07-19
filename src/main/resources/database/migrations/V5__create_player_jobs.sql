CREATE TABLE IF NOT EXISTS player_jobs (
    uuid TEXT NOT NULL,
    job_id TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 1,
    experience INTEGER NOT NULL DEFAULT 0,
    joined_at INTEGER NOT NULL DEFAULT (unixepoch('now')),
    PRIMARY KEY (uuid, job_id),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE INDEX idx_player_jobs_uuid ON player_jobs(uuid);