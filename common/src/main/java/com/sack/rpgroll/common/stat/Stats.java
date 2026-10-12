package com.sack.rpgroll.common.stat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

/** Lo que suman todas las {@link StatSource} instaladas. Sin ninguna, 0. */
public final class Stats {

    /** Maná máximo, en puntos. Lo aplica el core. */
    public static final String MANA = "mana";
    /** Daño de los hechizos, en % (10 = un 10 % más). Lo aplica Magic. */
    public static final String MAGIC_POWER = "magic_power";
    /** Daño de hechizo recibido, en % menos. Lo aplica Magic. */
    public static final String MAGIC_RESISTANCE = "magic_resistance";

    private Stats() {
    }

    public static double bonus(Player player, String stat) {

        if (Bukkit.getServer() == null) {
            return 0;
        }

        double total = 0;

        for (RegisteredServiceProvider<StatSource> source : Bukkit.getServicesManager()
                .getRegistrations(StatSource.class)) {
            total += source.getProvider().bonus(player, stat);
        }

        return total;
    }

    /** Bukkit la da de baja sola cuando {@code owner} se deshabilita. */
    public static void register(Plugin owner, StatSource source) {
        Bukkit.getServicesManager().register(StatSource.class, source, owner, ServicePriority.Normal);
    }

}
