package com.sack.rpgroll.worldforge.api;

/**
 * Something a generation provider really controls. A profile field that needs a capability
 * its provider lacks is rejected, never silently ignored.
 */
public enum Capability {
    SEED("semilla"),
    ENVIRONMENT("entorno (normal, nether, end)"),
    WORLD_TYPE("tipo de mundo vanilla (normal, amplificado, biomas grandes)"),
    STRUCTURES("estructuras vanilla on/off"),
    SPAWN("punto de aparición fijo"),
    PLATFORM("plataforma inicial"),
    LAYERS("capas de terreno plano"),
    BIOMES("elegir biomas existentes"),
    ORES("reglas de menas en chunks nuevos"),
    TEMPLATES("plantillas .nbt en chunks nuevos");

    private final String label;

    Capability(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
