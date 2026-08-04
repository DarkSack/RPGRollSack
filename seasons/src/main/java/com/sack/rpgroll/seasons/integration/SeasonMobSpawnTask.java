package com.sack.rpgroll.seasons.integration;

import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonMobModifier;
import com.sack.rpgroll.seasons.runtime.RegionSeasonResolver;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Por cada jugador online, sortea los {@code mobModifiers} de su estación
 * efectiva y hace spawnear mobs de RPGRoll-Mobs cerca suyo — no hace nada
 * si el addon no está instalado ({@link MobsIntegration#isAvailable()}).
 */
public class SeasonMobSpawnTask implements Runnable {

    private static final int RADIUS = 20;

    private final RegionSeasonResolver regionResolver;
    private final Random random = new Random();

    public SeasonMobSpawnTask(RegionSeasonResolver regionResolver) {
        this.regionResolver = regionResolver;
    }

    @Override
    public void run() {

        if (!MobsIntegration.isAvailable()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            processPlayer(player);
        }
    }

    private void processPlayer(Player player) {

        Season season = regionResolver.resolveSeason(player.getLocation()).orElse(null);

        if (season == null || season.mobModifiers().isEmpty()) {
            return;
        }

        for (SeasonMobModifier modifier : season.mobModifiers()) {
            if (random.nextDouble() < modifier.extraSpawnChance()) {
                MobsIntegration.spawnMob(modifier.mobId(), randomGroundNear(player.getLocation()));
            }
        }
    }

    private Location randomGroundNear(Location origin) {

        int dx = random.nextInt(RADIUS * 2 + 1) - RADIUS;
        int dz = random.nextInt(RADIUS * 2 + 1) - RADIUS;
        Location base = origin.clone().add(dx, 0, dz);
        int topY = base.getWorld().getHighestBlockYAt(base);

        return new Location(base.getWorld(), base.getX() + 0.5, topY + 1, base.getZ() + 0.5);
    }

}
