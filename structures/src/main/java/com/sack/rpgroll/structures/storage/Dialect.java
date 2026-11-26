package com.sack.rpgroll.structures.storage;

/** Base de datos soportada. Solo cambian los tipos de las tablas y el «insertar si no existe». */
public enum Dialect {
    SQLITE("TEXT", "INSERT OR IGNORE", "INTEGER PRIMARY KEY AUTOINCREMENT"),
    MYSQL("VARCHAR(64)", "INSERT IGNORE", "BIGINT PRIMARY KEY AUTO_INCREMENT");

    private final String shortText;
    private final String insertIgnore;
    private final String autoId;

    Dialect(String shortText, String insertIgnore, String autoId) {
        this.autoId = autoId;
        this.shortText = shortText;
        this.insertIgnore = insertIgnore;
    }

    /** Tipo de columna para textos que van en claves e índices (MySQL no indexa TEXT sin largo). */
    String shortText() {
        return shortText;
    }

    String insertIgnore() {
        return insertIgnore;
    }

    /** Columna numérica de clave primaria que se autonumera. */
    String autoId() {
        return autoId;
    }
}
