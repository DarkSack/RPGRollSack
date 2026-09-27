CREATE TABLE IF NOT EXISTS player_traits (
    uuid TEXT NOT NULL,
    trait_id TEXT NOT NULL,
    acquired_at BIGINT NOT NULL DEFAULT (extract(epoch FROM now())::bigint),
    PRIMARY KEY (uuid, trait_id),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_player_traits_uuid ON player_traits(uuid);
