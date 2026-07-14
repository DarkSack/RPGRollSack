CREATE TABLE IF NOT EXISTS players (

    uuid TEXT PRIMARY KEY,

    username TEXT NOT NULL,

    race TEXT,

    class TEXT,

    level INTEGER DEFAULT 1,

    experience INTEGER DEFAULT 0,

    created_at INTEGER,

    last_login INTEGER

);