package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/**
 * El monitor de bolsillo: con clic derecho abre la lista de cámaras (las tuyas y las de tu equipo)
 * desde donde estés, sin tener que ir al bloque del monitor. No se coloca: es papel con el modelo
 * de una tableta.
 */
public final class PortableMonitor {

    /** El id con el que se pide en {@code /trapadmin turret give}. */
    public static final String ID = "portable_monitor";

    private PortableMonitor() {
    }

    private static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "portable-monitor");
    }

    public static ItemStack create(Plugin plugin, LangManager lang, int amount) {

        ItemStack item = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtils.parse(lang.raw("camera.portable_name")).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(ComponentUtils.parse(lang.raw("camera.portable_lore")).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        // Su propio modelo (una tableta de mano, infra/objetos: formas_trampas.tableta), no el del monitor.
        meta.setItemModel(new NamespacedKey("rpgroll_traps", "turret/portable_monitor"));
        item.setItemMeta(meta);

        return item;
    }

    public static boolean is(Plugin plugin, ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }
}
