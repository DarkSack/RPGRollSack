CREATE TABLE IF NOT EXISTS player_skills (
    uuid TEXT NOT NULL,
    skill_id TEXT NOT NULL,
    skill_level INTEGER NOT NULL DEFAULT 1,
    learned_at BIGINT NOT NULL DEFAULT (extract(epoch FROM now())::bigint),
    PRIMARY KEY (uuid, skill_id),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_player_skills_uuid ON player_skills(uuid);
