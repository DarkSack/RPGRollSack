package com.sack.rpgroll.common.assets;

import com.sack.rpgroll.sackresourcepack.api.AssetTypeRegistry;
import com.sack.rpgroll.sackresourcepack.api.AssetsAPI;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Empuja el contenido de {@code <dataFolder>/resourcepack/<namespace>/<tipo>/...}
 * de un plugin hacia SackResourcePack (si está instalado) usando su
 * {@link AssetsAPI} pública — generalización de lo que ya hacía
 * {@code items/.../pack/PackAssetSync.java} pero para un único directorio
 * por plugin (no una colección de "packs" con nombre, que es específico de
 * Items). Es un puente puramente de archivos: el módulo llamante no
 * mantiene su propia copia "en memoria" de qué texturas existen.
 * <p>
 * Softdepend: si SackResourcePack no está instalado, {@link #syncAll} no
 * hace nada.
 */
public class ModuleAssetSync {

    private static final Map<String, Supplier<AssetTypeRegistry>> TYPE_FOLDERS = Map.of(
            "textures", AssetsAPI::textures,
            "models", AssetsAPI::models,
            "sounds", AssetsAPI::sounds,
            "font", AssetsAPI::fonts,
            "particles", AssetsAPI::particles);

    private final Plugin plugin;
    private final String moduleId;

    public ModuleAssetSync(Plugin plugin, String moduleId) {
        this.plugin = plugin;
        this.moduleId = moduleId;
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("SackResourcePack") != null && AssetsAPI.isReady();
    }

    /** Recorre {@code resourcepack/} y sincroniza todo hacia SackResourcePack. Silencioso si no está instalado. */
    public void syncAll() {

        if (!isAvailable()) {
            return;
        }

        File resourcePackDir = new File(plugin.getDataFolder(), "resourcepack");

        if (!resourcePackDir.isDirectory()) {
            return;
        }

        File[] namespaces = resourcePackDir.listFiles(File::isDirectory);
        if (namespaces == null) {
            return;
        }

        int synced = 0;

        for (File namespaceDir : namespaces) {

            String namespace = namespaceDir.getName();

            for (var typeEntry : TYPE_FOLDERS.entrySet()) {

                File typeDir = new File(namespaceDir, typeEntry.getKey());

                if (!typeDir.isDirectory()) {
                    continue;
                }

                AssetTypeRegistry registry = typeEntry.getValue().get();
                synced += syncTypeFolder(typeDir, typeDir, namespace, registry);
            }
        }

        if (synced > 0) {
            plugin.getLogger().info("✔ " + synced + " asset(s) sincronizado(s) hacia SackResourcePack.");
        }
    }

    private int syncTypeFolder(File root, File current, String namespace, AssetTypeRegistry registry) {

        File[] files = current.listFiles();
        if (files == null) {
            return 0;
        }

        int synced = 0;

        for (File file : files) {

            if (file.isDirectory()) {
                synced += syncTypeFolder(root, file, namespace, registry);
                continue;
            }

            String relativePath = root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');

            try {
                registry.register(moduleId, namespace, relativePath, Files.readAllBytes(file.toPath()));
                synced++;
            } catch (IOException e) {
                plugin.getLogger().warning("✘ No se pudo leer '" + file.getPath() + "': " + e.getMessage());
            }
        }

        return synced;
    }

}
