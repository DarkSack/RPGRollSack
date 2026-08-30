package com.sack.rpgroll.dungeons.structure;

import com.sack.rpgroll.dungeons.structure.schematic.SchematicBridge;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Trae a la Biblioteca una estructura guardada con un Structure Block
 * vanilla: el admin construye en survival, la guarda con nombre
 * {@code namespace:nombre} (o solo {@code nombre}, que Minecraft
 * namespacea como {@code minecraft:nombre}), y este servicio copia el
 * .nbt real ({@code Bukkit#getStructureManager()}, sección "estructuras
 * físicas") a {@code structures/<id>.nbt} + escribe el YAML de metadata
 * NATIVE — sin comandos externos, sin WorldEdit.
 */
public class StructureImportService {

    public enum Result {
        OK,
        VANILLA_NOT_FOUND,
        ID_TAKEN,
        IO_ERROR,
        WORLDEDIT_MISSING,
        FILE_NOT_FOUND
    }

    private final JavaPlugin dungeonsPlugin;
    private final StructureLibrary library;

    public StructureImportService(JavaPlugin dungeonsPlugin, StructureLibrary library) {
        this.dungeonsPlugin = dungeonsPlugin;
        this.library = library;
    }

    public Result importFromVanilla(String vanillaName, String newId, String displayName, Player requestedBy) {

        if (library.exists(newId)) {
            return Result.ID_TAKEN;
        }

        NamespacedKey key = NamespacedKey.fromString(vanillaName.toLowerCase(Locale.ROOT));

        if (key == null) {
            return Result.VANILLA_NOT_FOUND;
        }

        StructureManager manager = Bukkit.getStructureManager();
        Structure structure = manager.getStructure(key);

        if (structure == null) {
            try {
                structure = manager.loadStructure(key);
            } catch (Exception e) {
                structure = null;
            }
        }

        if (structure == null) {
            return Result.VANILLA_NOT_FOUND;
        }

        File destination = new File(dungeonsPlugin.getDataFolder(), "structures/" + newId + ".nbt");
        File parent = destination.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            manager.saveStructure(destination, structure);
        } catch (IOException e) {
            dungeonsPlugin.getLogger().warning("✘ Error guardando estructura importada '" + newId + "': "
                    + e.getMessage());
            return Result.IO_ERROR;
        }

        writeMetadata(newId, displayName, requestedBy, StructureSourceType.NATIVE, "Importada desde un Structure Block");
        library.reload();

        return Result.OK;
    }

    /**
     * Trae a la Biblioteca un schematic {@code .schem} de WorldEdit. Busca
     * el archivo en la carpeta {@code schematics/} del propio WorldEdit
     * (el mismo lugar donde {@code //schematic save}/{@code //schematic load}
     * leen y escriben), lo copia a {@code structures/<id>.schem} — para que
     * la structure de Dungeons no dependa de que el archivo original siga
     * existiendo ahí — y escribe el YAML de metadata SCHEMATIC.
     */
    public Result importFromSchematic(String schematicFileName, String newId, String displayName,
            Player requestedBy) {

        if (library.exists(newId)) {
            return Result.ID_TAKEN;
        }

        if (!SchematicBridge.isAvailable()) {
            return Result.WORLDEDIT_MISSING;
        }

        Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
        String fileName = schematicFileName.toLowerCase(Locale.ROOT).endsWith(".schem")
                ? schematicFileName
                : schematicFileName + ".schem";

        File source = new File(worldEdit.getDataFolder(), "schematics/" + fileName);

        if (!source.isFile()) {
            return Result.FILE_NOT_FOUND;
        }

        File destination = new File(dungeonsPlugin.getDataFolder(), "structures/" + newId + ".schem");
        File parent = destination.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            dungeonsPlugin.getLogger().warning("✘ Error copiando schematic importado '" + newId + "': "
                    + e.getMessage());
            return Result.IO_ERROR;
        }

        writeMetadata(newId, displayName, requestedBy, StructureSourceType.SCHEMATIC,
                "Importada desde el schematic '" + schematicFileName + "'");
        library.reload();

        return Result.OK;
    }

    private void writeMetadata(String id, String displayName, Player requestedBy, StructureSourceType source,
            String descriptionPrefix) {

        File metaFile = new File(dungeonsPlugin.getDataFolder(), "structures/" + id + ".yml");

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", id);
        config.set("display-name", (displayName == null || displayName.isBlank()) ? id : displayName);
        config.set("description", descriptionPrefix + (requestedBy != null ? " por " + requestedBy.getName() : "") + ".");
        config.set("icon", "MOSSY_COBBLESTONE");
        config.set("source", source.name());

        try {
            config.save(metaFile);
        } catch (IOException e) {
            dungeonsPlugin.getLogger().warning("✘ Error escribiendo metadata de '" + id + "': " + e.getMessage());
        }
    }

}
