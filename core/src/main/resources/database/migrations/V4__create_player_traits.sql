CREATE TABLE IF NOT EXISTS player_traits (
    uuid TEXT NOT NULL,
    trait_id TEXT NOT NULL,
    acquired_at INTEGER NOT NULL DEFAULT (unixepoch('now')),
    PRIMARY KEY (uuid, trait_id),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE INDEX idx_player_traits_uuid ON player_traits(uuid);
