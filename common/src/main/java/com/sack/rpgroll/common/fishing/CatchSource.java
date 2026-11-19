package com.sack.rpgroll.common.fishing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.Optional;

/**
 * Capturas sin nadie pescando: un plugin de pesca (RPGRoll-Fishing) sabe sacar uno de sus peces para
 * una máquina, como la Pescadora de RPGRoll-Machines. Se publica en el {@code ServicesManager} de
 * Bukkit ({@link #register}) y quien la usa la pide con {@link #get}; si no hay, pesca lo suyo.
 */
public interface CatchSource {

    /**
     * Un pez de este plugin pescado en {@code water} a nombre de {@code owner}, con calidad como mucho
     * {@code maxQuality} (0 = la más baja). Vacío si la tirada no da un pez (tesoro, basura o nada).
     */
    Optional<ItemStack> roll(Player owner, Location water, int maxQuality);

    static void register(Plugin owner, CatchSource source) {
        Bukkit.getServicesManager().register(CatchSource.class, source, owner, ServicePriority.Normal);
    }

    static Optional<CatchSource> get() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(CatchSource.class));
    }
}
