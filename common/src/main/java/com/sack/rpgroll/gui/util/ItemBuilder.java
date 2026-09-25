package com.sack.rpgroll.gui.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Utilidad para crear ItemStacks para GUIs.
 */
public class ItemBuilder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder setName(Component name) {
        meta.displayName(name);
        return this;
    }

    public ItemBuilder setLore(Component... lore) {
        meta.lore(Arrays.asList(lore));
        return this;
    }

    public ItemBuilder setLore(List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Convierte un string que puede contener saltos de línea (\n) en una lista
     * de Components, uno por línea, aplicando color codes en cada una.
     * Útil para campos YAML escritos como bloque literal (usando "|").
     */
    public static List<Component> toLoreLines(String raw) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();

        for (String line : raw.split("\n")) {
            lines.add(MINI.deserialize(line));
        }

        return lines;
    }

    /**
     * Crea un builder de cabeza de jugador (PLAYER_HEAD) con una textura
     * custom aplicada vía código base64 (ej. obtenido de minecraft-heads.com).
     * <p>
     * Si el texture es null o está vacío, devuelve una cabeza sin textura
     * custom (silueta por defecto) — no lanza excepción.
     */
    public static ItemBuilder skull(String base64Texture) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
        builder.applySkullTexture(base64Texture);
        return builder;
    }

    private void applySkullTexture(String base64Texture) {

        if (base64Texture == null || base64Texture.isBlank()) {
            return;
        }

        if (!(meta instanceof SkullMeta skullMeta)) {
            return;
        }

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", base64Texture));
        skullMeta.setPlayerProfile(profile);
    }

    public static ItemStack createFiller() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(Component.text(" ", NamedTextColor.GRAY))
                .build();
    }

    public static ItemStack createConfirmButton(String text) {
        return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .setName(Component.text("✔ " + text, NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .build();
    }

    public static ItemStack createCancelButton(String text) {
        return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName(Component.text("✖ " + text, NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .build();
    }

}