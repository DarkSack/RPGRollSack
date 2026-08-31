package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * El ítem colocable de una torreta.
 * <p>
 * Lleva el id de la definición en su PersistentDataContainer, así que un
 * jugador puede tenerla en el inventario, ponerla donde quiera y recogerla
 * después. El material que se ve es el {@code baseBlock} de la torreta —
 * lo mismo que va a quedar en el mundo — para que se entienda qué se está
 * colocando antes de hacerlo.
 */
public final class TurretItem {

    private static final String KEY = "turret-id";

    private TurretItem() {
    }

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, KEY);
    }

    /** Crea el ítem de una torreta, listo para entregar. */
    public static ItemStack create(Plugin plugin, TurretDefinition definition, LangManager lang, int amount) {

        // Un ítem tiene que ser colocable: si la torreta no define bloque
        // base, se usa un dispensador, que es lo que más se parece.
        Material material = definition.baseBlock() != null ? definition.baseBlock() : Material.DISPENSER;

        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtils.parse(definition.displayName())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        if (!definition.description().isBlank()) {
            lore.add(ComponentUtils.parse(definition.description()).decoration(TextDecoration.ITALIC, false));
        }

        lore.add(ComponentUtils.parse(lang.raw("admin.turret.item_lore_place"))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(ComponentUtils.parse(lang.raw("admin.turret.item_lore_owner"))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, definition.id());

        item.setItemMeta(meta);
        return item;
    }

    /** El id de torreta que lleva el ítem, o null si no es un ítem de torreta. */
    public static String turretIdOf(Plugin plugin, ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        return item.getItemMeta().getPersistentDataContainer()
                .get(key(plugin), PersistentDataType.STRING);
    }

}
