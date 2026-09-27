package com.sack.rpgroll.database;

/**
 * El motor de base de datos de un {@link DatabaseProvider}. Las consultas de RPGRoll están escritas
 * para valer en los dos (los upserts con {@code ON CONFLICT} los entiende SQLite desde la 3.24); lo
 * que cambia son las migraciones, que van en carpetas separadas.
 */
public enum DatabaseDialect {

    SQLITE,

    POSTGRESQL

}
