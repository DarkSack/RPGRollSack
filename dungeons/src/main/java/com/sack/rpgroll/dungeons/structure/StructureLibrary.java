package com.sack.rpgroll.dungeons.structure;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Carga la Biblioteca de Estructuras desde
 * {@code plugins/RPGRoll-Dungeons/structures/*.yml} — cada entrada NATIVE
 * espera además un {@code structures/<id>.nbt} junto al YAML, que este
 * manager no lee directamente (eso lo resuelve {@link StructurePasteEngine}
 * o {@link StructureImportService} cuando corresponde).
 */
public class StructureLibrary extends ContentManager<StructureDefinition> {

    public StructureLibrary(JavaPlugin dungeonsPlugin) {
        super(owningPlugin(), new YamlLoader(dungeonsPlugin), "structures", "estructura",
                new StructureParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(StructureLibrary.class);
    }

}
