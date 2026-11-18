package com.sack.rpgroll.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menús dibujados de un módulo: el prefijo del paquete de recursos donde están sus fondos y sus
 * modelos (por ejemplo {@code sackito:menus}). Con prefijo, el menú {@code id} usa la fuente
 * {@code <prefijo>/<id>} como fondo y los botones ya dibujados llevan el modelo
 * {@code <prefijo>/invisible}. Sin prefijo (de fábrica), los menús se ven como siempre.
 * <p>
 * Cada módulo lee {@code menus.pack} de su config y lo registra con {@link #register} al arrancar y al
 * recargar; sus menús lo piden con {@link #get}.
 */
public record MenuSkin(String pack) {

    public static final MenuSkin OFF = new MenuSkin("");

    private static final String INVISIBLE = "invisible";
    private static final Map<String, MenuSkin> SKINS = new ConcurrentHashMap<>();

    public MenuSkin {
        pack = pack == null ? "" : pack.trim().toLowerCase(Locale.ROOT);
    }

    /** Lee {@code pack} de la sección {@code menus} de una config (null = sin menús dibujados). */
    public static MenuSkin from(ConfigurationSection menus) {
        return of(menus == null ? null : menus.getString("pack", ""));
    }

    public static MenuSkin of(String pack) {
        MenuSkin skin = new MenuSkin(pack);
        return skin.drawn() ? skin : OFF;
    }

    /** Guarda el skin de un módulo ({@code core}, {@code economy}, {@code pass}, {@code recipes}...). */
    public static void register(String module, MenuSkin skin) {
        SKINS.put(module, skin == null ? OFF : skin);
    }

    /** El skin del módulo, o {@link #OFF} si no registró ninguno. */
    public static MenuSkin get(String module) {
        return SKINS.getOrDefault(module, OFF);
    }

    public boolean drawn() {
        return !pack.isEmpty() && Key.parseable(pack);
    }

    /** Clave de la fuente del fondo {@code id}, o "" sin menús dibujados. */
    public String background(String id) {
        return drawn() ? pack + "/" + id : "";
    }

    /** Modelo del relleno y de los botones dibujados, o null sin menús dibujados. */
    public String fillerModel() {
        return drawn() ? pack + "/" + INVISIBLE : null;
    }

    /** El título con el fondo {@code id} delante, o el mismo título sin menús dibujados. */
    public Component title(String id, Component title) {
        return MenuBackground.title(background(id), title);
    }

    /** Botón cuyo dibujo ya está en el fondo: invisible con fondo (con su nombre y lore), igual sin él. */
    public ItemStack button(ItemStack item) {
        if (!drawn() || item == null) {
            return item;
        }
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString(fillerModel()));
        copy.setItemMeta(meta);
        return copy;
    }

    /** Relleno: invisible y sin recuadro de nombre con fondo, {@code plain} sin él. */
    public ItemStack filler(ItemStack plain) {
        if (!drawn()) {
            return plain;
        }
        ItemStack item = ItemStack.of(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString(fillerModel()));
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        return item;
    }
}
