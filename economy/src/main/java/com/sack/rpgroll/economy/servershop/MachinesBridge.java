package com.sack.rpgroll.economy.servershop;

import com.sack.rpgroll.machines.MachinesPlugin;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * La única clase de la tienda que toca RPGRoll-Machines: solo se carga si ese
 * plugin está activo, así que sin él no hay NoClassDefFoundError.
 */
final class MachinesBridge {

    private MachinesBridge() {
    }

    /** {@code mob}: {@code minecraft:zombie}. Vacío si el mob no existe o no puede salir de un spawner. */
    static Optional<ItemStack> spawner(String mob) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Machines") instanceof MachinesPlugin machines)) {
            return Optional.empty();
        }
        return machines.createSpawner(mob);
    }

}
