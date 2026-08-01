CREATE TABLE IF NOT EXISTS placed_blocks (
    world TEXT NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    PRIMARY KEY (world, x, y, z)
);