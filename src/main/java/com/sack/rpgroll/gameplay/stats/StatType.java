package com.sack.rpgroll.gameplay.stats;

import java.util.Map;

public enum StatType {
    STRENGTH("strength", "str", "fuerza"),
    DEXTERITY("dexterity", "dex", "destreza"),
    CONSTITUTION("constitution", "con", "constitucion"),
    INTELLIGENCE("intelligence", "int", "inteligencia"),
    WISDOM("wisdom", "wis", "sabiduria"),
    CHARISMA("charisma", "cha", "carisma");

    private final String[] aliases;

    StatType(String... aliases) {
        this.aliases = aliases;
    }

    private static final Map<String, StatType> LOOKUP = buildLookup();

    private static Map<String, StatType> buildLookup() {
        Map<String, StatType> map = new java.util.HashMap<>();
        for (StatType type : values()) {
            for (String alias : type.aliases) {
                map.put(alias, type);
            }
        }
        return Map.copyOf(map);
    }

    /**
     * Resuelve un StatType a partir de un nombre o alias (case-insensitive).
     *
     * @return el StatType correspondiente, o {@code null} si no existe
     */
    public static StatType fromString(String value) {
        if (value == null) {
            return null;
        }
        return LOOKUP.get(value.toLowerCase());
    }
}