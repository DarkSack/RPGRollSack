package com.sack.rpgroll.structures.integration;

import java.util.Locale;

/** Plugins con los que Structures se integra de forma opcional. */
public enum Hook {
    WORLDGUARD("WorldGuard"),
    GRIEFPREVENTION("GriefPrevention"),
    VAULT("Vault"),
    FLOODGATE("floodgate"),
    MYTHICMOBS("MythicMobs");

    private final String plugin;

    Hook(String plugin) {
        this.plugin = plugin;
    }

    /** Nombre del plugin en el servidor. */
    public String plugin() {
        return plugin;
    }

    /** Clave en integrations.yml. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
