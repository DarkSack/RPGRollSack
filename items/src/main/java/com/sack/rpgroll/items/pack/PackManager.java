package com.sack.rpgroll.items.pack;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Administra las carpetas de {@code packs/<nombre>/} — cada una es una
 * "colección" de ítems que se guarda y organiza junta, y trae su propia
 * {@code resourcepack/} para las texturas de esos ítems (ver
 * {@link com.sack.rpgroll.items.pack.PackAssetSync}). No conoce nada de
 * {@link com.sack.rpgroll.items.core.ItemDefinition} — solo maneja carpetas
 * en disco.
 */
public class PackManager {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-z0-9_-]+$");

    private final Plugin plugin;

    public PackManager(Plugin plugin) {
        this.plugin = plugin;
    }

    private File packsRoot() {
        return new File(plugin.getDataFolder(), "packs");
    }

    public boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    public boolean exists(String name) {
        return new File(packsRoot(), name).isDirectory();
    }

    /** @return nombres de todos los packs existentes, ordenados alfabéticamente. */
    public List<String> list() {

        File root = packsRoot();
        File[] entries = root.listFiles(File::isDirectory);

        if (entries == null) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (File entry : entries) {
            names.add(entry.getName());
        }

        names.sort(Comparator.naturalOrder());
        return names;
    }

    /**
     * Crea {@code packs/<name>/} y su {@code resourcepack/} vacía si no
     * existían. No falla si ya existe — es idempotente.
     */
    public void create(String name) {

        File packDir = new File(packsRoot(), name);
        File resourcePackDir = new File(packDir, "resourcepack");

        if (!resourcePackDir.exists() && !resourcePackDir.mkdirs()) {
            plugin.getLogger().warning("✘ No se pudo crear la carpeta del pack: " + packDir.getPath());
        }
    }

    /**
     * Migración de una sola vez: si todavía existe la vieja carpeta
     * {@code items/<categoria>/*.yml} (esquema pre-packs), mueve cada
     * subcarpeta a {@code packs/<mismo-nombre>/} tal cual, reescribiendo
     * la clave {@code category:} a {@code pack:} en cada archivo movido
     * (preservando el resto del contenido/formato — no se re-serializa el
     * YAML entero). Es seguro llamarla en cada arranque: si {@code items/}
     * ya no existe, no hace nada.
     */
    public void migrateLegacyItemsFolder() {

        File legacyRoot = new File(plugin.getDataFolder(), "items");

        if (!legacyRoot.isDirectory()) {
            return;
        }

        File[] categoryDirs = legacyRoot.listFiles(File::isDirectory);

        if (categoryDirs == null || categoryDirs.length == 0) {
            legacyRoot.delete();
            return;
        }

        int migratedFiles = 0;

        for (File categoryDir : categoryDirs) {

            String packName = categoryDir.getName();
            create(packName);

            File[] ymlFiles = categoryDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));

            if (ymlFiles == null) {
                continue;
            }

            for (File file : ymlFiles) {
                if (migrateFile(file, new File(new File(plugin.getDataFolder(), "packs/" + packName), file.getName()))) {
                    migratedFiles++;
                }
            }

            categoryDir.delete();
        }

        legacyRoot.delete();

        if (migratedFiles > 0) {
            plugin.getLogger().info("✔ Migración a packs/: " + migratedFiles
                    + " ítem(s) movido(s) desde el viejo esquema items/<categoria>/.");
        }
    }

    private boolean migrateFile(File source, File target) {

        try {

            List<String> lines = Files.readAllLines(source.toPath());
            List<String> rewritten = new ArrayList<>(lines.size());

            for (String line : lines) {
                rewritten.add(line.startsWith("category:") ? "pack:" + line.substring("category:".length()) : line);
            }

            Files.write(target.toPath(), rewritten);
            Files.deleteIfExists(source.toPath());
            return true;

        } catch (IOException e) {
            plugin.getLogger().warning(
                    "✘ No se pudo migrar '" + source.getPath() + "' a packs/: " + e.getMessage());
            return false;
        }
    }

}
