package com.sack.rpgroll.items.pack;

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
 * Empuja el contenido de {@code packs/<pack>/resourcepack/<namespace>/<tipo>/...}
 * hacia SackResourcePack (si está instalado) usando su {@link AssetsAPI}
 * pública — el mismo pipeline de escaneo/merge/build que ya usa cualquier
 * otro módulo de contenido. Es un puente puramente de archivos: RPGRoll-Items
 * no mantiene su propia copia "en memoria" de qué texturas existen.
 * <p>
 * Softdepend: si SackResourcePack no está instalado, {@link #syncAll} no
 * hace nada — los packs siguen funcionando igual, solo sin distribución
 * automática de sus texturas.
 */
public class PackAssetSync {

    private static final Map<String, Supplier<AssetTypeRegistry>> TYPE_FOLDERS = Map.of(
            "textures", AssetsAPI::textures,
            "models", AssetsAPI::models,
            "sounds", AssetsAPI::sounds,
            "font", AssetsAPI::fonts,
            "particles", AssetsAPI::particles);

    private final Plugin plugin;
    private final PackManager packManager;

    public PackAssetSync(Plugin plugin, PackManager packManager) {
        this.plugin = plugin;
        this.packManager = packManager;
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("SackResourcePack") != null && AssetsAPI.isReady();
    }

    /** Recorre todos los packs y sincroniza sus resourcepack/ hacia SackResourcePack. Silencioso si no está instalado. */
    public void syncAll() {

        if (!isAvailable()) {
            return;
        }

        int synced = 0;

        for (String pack : packManager.list()) {
            synced += syncPack(pack);
        }

        if (synced > 0) {
            plugin.getLogger().info("✔ " + synced + " asset(s) sincronizado(s) hacia SackResourcePack.");
        }
    }

    private int syncPack(String pack) {

        File resourcePackDir = new File(new File(plugin.getDataFolder(), "packs/" + pack), "resourcepack");

        if (!resourcePackDir.isDirectory()) {
            return 0;
        }

        File[] namespaces = resourcePackDir.listFiles(File::isDirectory);
        if (namespaces == null) {
            return 0;
        }

        String moduleId = "items_" + pack;
        int synced = 0;

        for (File namespaceDir : namespaces) {

            String namespace = namespaceDir.getName();

            for (var typeEntry : TYPE_FOLDERS.entrySet()) {

                File typeDir = new File(namespaceDir, typeEntry.getKey());

                if (!typeDir.isDirectory()) {
                    continue;
                }

                AssetTypeRegistry registry = typeEntry.getValue().get();
                synced += syncTypeFolder(typeDir, typeDir, moduleId, namespace, registry);
            }
        }

        return synced;
    }

    private int syncTypeFolder(File root, File current, String moduleId, String namespace,
            AssetTypeRegistry registry) {

        File[] files = current.listFiles();
        if (files == null) {
            return 0;
        }

        int synced = 0;

        for (File file : files) {

            if (file.isDirectory()) {
                synced += syncTypeFolder(root, file, moduleId, namespace, registry);
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
