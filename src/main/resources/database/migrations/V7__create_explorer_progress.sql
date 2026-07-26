CREATE TABLE IF NOT EXISTS explorer_biomes (
    uuid TEXT NOT NULL,
    biome TEXT NOT NULL,
    PRIMARY KEY (uuid, biome),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS explorer_distance (
    uuid TEXT PRIMARY KEY,
    distance_since_payout REAL NOT NULL DEFAULT 0,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE INDEX idx_explorer_biomes_uuid ON explorer_biomes(uuid);
CREATE INDEX idx_explorer_distance_uuid ON explorer_distance(uuid);