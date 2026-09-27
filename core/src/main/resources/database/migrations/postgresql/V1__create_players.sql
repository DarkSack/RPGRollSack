-- PostgreSQL: los instantes son milisegundos, en BIGINT (en PostgreSQL INTEGER es de 32 bits).
CREATE TABLE IF NOT EXISTS players (
    uuid TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    race TEXT,
    class TEXT,
    level INTEGER DEFAULT 1,
    experience INTEGER DEFAULT 0,
    created_at BIGINT,
    last_login BIGINT
);
