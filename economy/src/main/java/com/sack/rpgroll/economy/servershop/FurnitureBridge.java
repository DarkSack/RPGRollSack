package com.sack.rpgroll.economy.servershop;

import com.sack.rpgroll.furniture.FurniturePlugin;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * La única clase de la tienda que toca RPGRoll-Furniture: solo se carga si ese
 * plugin está activo, así que sin él no hay NoClassDefFoundError.
 */
final class FurnitureBridge {

    private FurnitureBridge() {
    }

    /** {@code ref}: id del mueble, o id:versión (chair:spruce). */
    static Optional<ItemStack> create(String ref) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Furniture") instanceof FurniturePlugin furniture)) {
            return Optional.empty();
        }
        return furniture.createItem(ref, 1);
    }

}
