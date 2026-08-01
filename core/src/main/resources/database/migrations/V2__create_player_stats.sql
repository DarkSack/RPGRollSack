CREATE TABLE IF NOT EXISTS player_stats (

    uuid TEXT PRIMARY KEY,

    strength INTEGER DEFAULT 10,

    dexterity INTEGER DEFAULT 10,

    constitution INTEGER DEFAULT 10,

    intelligence INTEGER DEFAULT 10,

    wisdom INTEGER DEFAULT 10,

    charisma INTEGER DEFAULT 10,

    FOREIGN KEY(uuid) REFERENCES players(uuid)

);