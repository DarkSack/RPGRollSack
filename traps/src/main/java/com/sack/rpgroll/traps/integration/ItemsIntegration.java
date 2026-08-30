package com.sack.rpgroll.traps.integration;

import com.sack.rpgroll.items.ItemsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Integración blanda con RPGRoll-Items para la "llave" de un bloque
 * protegido: si el addon está presente y {@code requiredItemId} coincide
 * con una definición suya, se reconoce por id de ítem custom; si no, se
 * interpreta como un {@link Material} vanilla directo en la mano del
 * jugador. Softdepend real — nunca crashea si RPGRoll-Items no está
 * instalado (mismo criterio que {@code ItemsIntegration} de Dungeons).
 */
public final class ItemsIntegration {

    private ItemsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Items")
                && Bukkit.getPluginManager().getPlugin("RPGRoll-Items") instanceof ItemsPlugin;
    }

    /** @return true si {@code player} tiene en la mano el ítem que abre esta llave. */
    public static boolean hasKey(Player player, String requiredItemId) {

        if (requiredItemId == null || requiredItemId.isBlank()) {
            return true;
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (isAvailable()) {

            var plugin = (ItemsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Items");
            var heldId = plugin.getInstanceService().getId(inHand);

            if (heldId.isPresent()) {
                return heldId.get().equalsIgnoreCase(requiredItemId);
            }
            // Si RPGRoll-Items no reconoce el ítem en mano, cae al fallback vanilla debajo.
        }

        try {
            Material required = Material.valueOf(requiredItemId.trim().toUpperCase(Locale.ROOT));
            return inHand.getType() == required;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
