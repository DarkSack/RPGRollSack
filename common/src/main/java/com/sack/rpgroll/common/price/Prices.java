package com.sack.rpgroll.common.price;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

/** Lo que suman todas las {@link PriceSource} instaladas. Sin ninguna, los precios no se mueven. */
public final class Prices {

    private Prices() {
    }

    /** Sin recortar: el tope lo pone quien cobra (ver ServerShopService en Economy). */
    public static double discount(Player player, String faction) {

        if (Bukkit.getServer() == null) {
            return 0;
        }

        double total = 0;

        for (RegisteredServiceProvider<PriceSource> source : Bukkit.getServicesManager()
                .getRegistrations(PriceSource.class)) {
            total += source.getProvider().discount(player, faction);
        }

        return total;
    }

    /** Bukkit la da de baja sola cuando {@code owner} se deshabilita. */
    public static void register(Plugin owner, PriceSource source) {
        Bukkit.getServicesManager().register(PriceSource.class, source, owner, ServicePriority.Normal);
    }

}
