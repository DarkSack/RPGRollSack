package com.sack.rpgroll.structures.storage;

/** Base de datos soportada. Solo cambian los tipos de las tablas y el «insertar si no existe». */
public enum Dialect {
    SQLITE("TEXT", "INSERT OR IGNORE"),
    MYSQL("VARCHAR(64)", "INSERT IGNORE");

    private final String shortText;
    private final String insertIgnore;

    Dialect(String shortText, String insertIgnore) {
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
}
