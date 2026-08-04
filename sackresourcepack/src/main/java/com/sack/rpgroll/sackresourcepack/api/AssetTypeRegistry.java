package com.sack.rpgroll.sackresourcepack.api;

import com.sack.rpgroll.sackresourcepack.event.AssetRegisterEvent;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Registro genérico para UN tipo de asset (modelos/texturas/sonidos/...)
 * — escribe directo en {@code content/<moduleId>/assets/<namespace>/<folder>/<relativePath>},
 * exactamente donde el {@code ManifestScanner} ya lo va a encontrar en el
 * próximo escaneo. Si el módulo todavía no tiene {@code pack.yml}, se le
 * crea uno mínimo automáticamente — no hace falta declarar el módulo a
 * mano solo para usar la API.
 */
public class AssetTypeRegistry {

    private final File contentDirectory;
    private final String folder;

    AssetTypeRegistry(File contentDirectory, String folder) {
        this.contentDirectory = contentDirectory;
        this.folder = folder;
    }

    /**
     * @param moduleId     carpeta bajo content/ que va a contener este asset — se crea si no existe
     * @param namespace    namespace del asset (ej. "rpgroll_fishing")
     * @param relativePath ruta relativa DENTRO del tipo, con extensión (ej. "item/fish_salmon.json")
     * @param content      bytes crudos del archivo
     */
    public void register(String moduleId, String namespace, String relativePath, byte[] content) {

        File moduleDirectory = new File(contentDirectory, moduleId);
        ensureManifest(moduleDirectory, moduleId, namespace);

        File target = new File(moduleDirectory, "assets/" + namespace + "/" + folder + "/" + relativePath);

        try {
            Files.createDirectories(target.getParentFile().toPath());
            Files.write(target.toPath(), content);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo registrar el asset '" + relativePath + "' del módulo '"
                    + moduleId + "'", e);
        }

        Bukkit.getPluginManager().callEvent(new AssetRegisterEvent(moduleId, moduleId));
    }

    private void ensureManifest(File moduleDirectory, String moduleId, String namespace) {

        File manifestFile = new File(moduleDirectory, "pack.yml");

        if (manifestFile.isFile()) {
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", moduleId);
        config.set("name", moduleId);
        config.set("namespace", namespace);
        config.set("version", "1.0.0");

        try {
            manifestFile.getParentFile().mkdirs();
            config.save(manifestFile);
        } catch (IOException ignored) {
        }
    }

}
