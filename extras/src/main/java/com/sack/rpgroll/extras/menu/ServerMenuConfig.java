package com.sack.rpgroll.extras.menu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;

/**
 * La sección {@code server-menu} de config.yml: el ítem que abre el menú del
 * servidor (una brújula por defecto) y cómo se comporta.
 *
 * @param slot        casilla de la barra (0-8) donde se entrega
 * @param locked      no se puede tirar, mover a otro inventario ni cambiar de mano
 * @param keepOnDeath no cae al morir y vuelve al reaparecer
 */
public record ServerMenuConfig(boolean enabled, String menuId, Material material, String name, List<String> lore,
        int slot, boolean glint, boolean giveOnJoin, boolean keepOnDeath, boolean locked) {

    public static final ServerMenuConfig DISABLED = new ServerMenuConfig(false, "servidor", Material.COMPASS, "",
            List.of(), 8, false, false, false, false);

    public static ServerMenuConfig from(ConfigurationSection section) {

        if (section == null) {
            return DISABLED;
        }

        Material material = Material.matchMaterial(section.getString("item.material", "COMPASS")
                .toUpperCase(Locale.ROOT));

        return new ServerMenuConfig(
                section.getBoolean("enabled", false),
                section.getString("menu", "servidor"),
                material == null || !material.isItem() ? Material.COMPASS : material,
                section.getString("item.name", "&c&lBrújula del Nether"),
                section.getStringList("item.lore"),
                Math.max(0, Math.min(8, section.getInt("item.slot", 8))),
                section.getBoolean("item.glint", true),
                section.getBoolean("give-on-join", true),
                section.getBoolean("keep-on-death", true),
                section.getBoolean("locked", true));
    }

}
