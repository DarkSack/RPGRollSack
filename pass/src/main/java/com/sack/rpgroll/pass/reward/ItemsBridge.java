package com.sack.rpgroll.pass.reward;

import com.sack.rpgroll.items.ItemsPlugin;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;

import java.util.Optional;

/**
 * La única clase del pase que toca RPGRoll-Items: solo se carga si ese plugin
 * está activo, así que sin él no hay NoClassDefFoundError.
 */
final class ItemsBridge {

    private ItemsBridge() {
    }

    /** El nombre con el que el jugador ve el ítem ("Poción de curación", no "healing_potion"). */
    static Optional<Component> name(String itemId) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Items") instanceof ItemsPlugin items)) {
            return Optional.empty();
        }

        return items.getItemManager().get(itemId)
                .map(definition -> items.getItemFactory().create(definition).effectiveName());
    }

}
