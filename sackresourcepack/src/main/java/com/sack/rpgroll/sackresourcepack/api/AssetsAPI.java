package com.sack.rpgroll.sackresourcepack.api;

import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * Punto de entrada público de SackResourcePack. Cualquier otro plugin
 * (del ecosistema RPGRoll o no) puede depender blandamente de este y
 * registrar sus propios assets sin tocar {@code content/} a mano —
 * {@link #assets()} para copiar un {@code resourcepack/} entero
 * embebido en el jar del plugin, o los registros por tipo
 * ({@link #models()}, {@link #textures()}, ...) para escribir archivos
 * sueltos. Todo converge en el mismo pipeline de escaneo/merge/build ya
 * existente — no hay un modelo de objetos "en memoria" paralelo.
 */
public final class AssetsAPI {

    private static AssetsAPI instance;

    private final AssetRegistrationService assets;
    private final AssetTypeRegistry models;
    private final AssetTypeRegistry textures;
    private final AssetTypeRegistry sounds;
    private final AssetTypeRegistry fonts;
    private final AssetTypeRegistry music;
    private final AssetTypeRegistry languages;
    private final AssetTypeRegistry gui;
    private final AssetTypeRegistry particles;

    private AssetsAPI(Plugin plugin, File contentDirectory) {
        this.assets = new AssetRegistrationService(plugin, contentDirectory);
        this.models = new AssetTypeRegistry(contentDirectory, "models");
        this.textures = new AssetTypeRegistry(contentDirectory, "textures");
        this.sounds = new AssetTypeRegistry(contentDirectory, "sounds");
        this.fonts = new AssetTypeRegistry(contentDirectory, "font");
        this.music = new AssetTypeRegistry(contentDirectory, "sounds/music");
        this.languages = new AssetTypeRegistry(contentDirectory, "lang");
        this.gui = new AssetTypeRegistry(contentDirectory, "textures/gui");
        this.particles = new AssetTypeRegistry(contentDirectory, "particles");
    }

    public static void init(Plugin plugin, File contentDirectory) {
        instance = new AssetsAPI(plugin, contentDirectory);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si SackResourcePack todavía no está listo. */
    public static AssetsAPI get() {

        if (instance == null) {
            throw new IllegalStateException("SackResourcePack todavía no está listo.");
        }

        return instance;
    }

    public static AssetRegistrationService assets() {
        return get().assets;
    }

    public static AssetTypeRegistry models() {
        return get().models;
    }

    public static AssetTypeRegistry textures() {
        return get().textures;
    }

    public static AssetTypeRegistry sounds() {
        return get().sounds;
    }

    public static AssetTypeRegistry fonts() {
        return get().fonts;
    }

    public static AssetTypeRegistry music() {
        return get().music;
    }

    public static AssetTypeRegistry languages() {
        return get().languages;
    }

    public static AssetTypeRegistry gui() {
        return get().gui;
    }

    public static AssetTypeRegistry particles() {
        return get().particles;
    }

}
