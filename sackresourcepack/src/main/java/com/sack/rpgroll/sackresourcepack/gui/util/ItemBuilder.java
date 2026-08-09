package com.sack.rpgroll.sackresourcepack.gui.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Helper mínimo de items para las GUIs de SackResourcePack — sin relación con el ItemBuilder de :core. */
public final class ItemBuilder {

    private ItemBuilder() {
    }

    public static ItemStack of(Material material, String name, NamedTextColor color, String... lore) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));

        if (lore.length > 0) {

            List<Component> loreLines = new ArrayList<>();

            for (String line : lore) {
                loreLines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(loreLines);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler() {

        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack backButton() {
        return of(Material.ARROW, "Volver", NamedTextColor.RED);
    }

    /**
     * Cabeza de jugador con la textura de {@code textureUrl} pintada sobre el
     * modelo 3D de cabeza — mismo truco que {@code ItemBuilder.skull(...)} de
     * :core, pero armando el payload nosotros mismos a partir de una URL (en
     * vez de un base64 ya hecho de minecraft-heads.com), porque acá la fuente
     * es el host HTTP propio de SackResourcePack sirviendo el PNG fusionado.
     * <p>
     * Es un preview parcial, no perfecto: solo funciona con texturas (no
     * modelos/sonidos/fuentes), requiere que el cliente del jugador pueda
     * alcanzar ese host por red, y la imagen se distorsiona al envolverse
     * sobre el UV de una cabeza en vez de mostrarse plana.
     */
    public static ItemStack skullFromUrl(String textureUrl, String name, NamedTextColor color, String... lore) {

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        if (meta instanceof SkullMeta skullMeta) {

            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
            String base64Texture = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", base64Texture));
            skullMeta.setPlayerProfile(profile);
        }

        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));

        if (lore.length > 0) {

            List<Component> loreLines = new ArrayList<>();

            for (String line : lore) {
                loreLines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(loreLines);
        }

        item.setItemMeta(meta);
        return item;
    }

}
