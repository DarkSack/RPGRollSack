-- V6 y V8 (placed_blocks) no están aquí: son del mundo de cada servidor y se quedan en su SQLite.
CREATE TABLE IF NOT EXISTS explorer_biomes (
    uuid TEXT NOT NULL,
    biome TEXT NOT NULL,
    PRIMARY KEY (uuid, biome),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS explorer_distance (
    uuid TEXT PRIMARY KEY,
    distance_since_payout DOUBLE PRECISION NOT NULL DEFAULT 0,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_explorer_biomes_uuid ON explorer_biomes(uuid);
