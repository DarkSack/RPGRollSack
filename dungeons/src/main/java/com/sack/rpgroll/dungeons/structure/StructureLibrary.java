package com.sack.rpgroll.dungeons.structure;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
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
        super(resolveCoreInstance(), new YamlLoader(dungeonsPlugin), "structures", "estructura",
                new StructureParser());
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
