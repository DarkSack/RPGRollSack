package com.sack.rpgroll.mobs.listener;

import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.core.SpawnRules;
import com.sack.rpgroll.mobs.engine.MobEngine;
import com.sack.rpgroll.mobs.region.MobRegionManager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;
import java.util.Locale;

/**
 * Intercepta spawns naturales vanilla y, si alguna definición con
 * {@code spawnRules.naturalSpawn: true} calza con el tipo de entidad y el
 * contexto (bioma/mundo/región/altura/hora/clima/distancia a jugadores),
 * cancela el spawn vanilla y crea el mob RPGRoll en su lugar (elegido por
 * peso entre las definiciones candidatas).
 */
public class NaturalSpawnListener implements Listener {

    private final MobManager mobManager;
    private final MobEngine engine;
    private final MobRegionManager regionManager;

    public NaturalSpawnListener(MobManager mobManager, MobEngine engine, MobRegionManager regionManager) {
        this.mobManager = mobManager;
        this.engine = engine;
        this.regionManager = regionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        List<MobDefinition> candidates = mobManager.getAll().stream()
                .filter(def -> def.spawnRules().naturalSpawn())
                .filter(def -> def.model().baseEntityType().equalsIgnoreCase(event.getEntityType().name()))
                .filter(def -> matches(def.spawnRules(), event.getLocation()))
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        MobDefinition chosen = pickWeighted(candidates);

        event.setCancelled(true);
        engine.spawnMob(chosen, event.getLocation());
    }

    private boolean matches(SpawnRules rules, Location location) {

        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        if (!rules.worlds().isEmpty()
                && rules.worlds().stream().noneMatch(w -> w.equalsIgnoreCase(world.getName()))) {
            return false;
        }

        if (location.getY() < rules.minHeight() || location.getY() > rules.maxHeight()) {
            return false;
        }

        if (!rules.biomes().isEmpty()) {
            String biome = location.getBlock().getBiome().getKey().getKey();
            if (rules.biomes().stream().noneMatch(b -> b.equalsIgnoreCase(biome))) {
                return false;
            }
        }

        if (!rules.regions().isEmpty()) {
            boolean insideAny = rules.regions().stream()
                    .map(regionManager::get)
                    .anyMatch(regionOpt -> regionOpt.isPresent() && regionOpt.get().contains(location));
            if (!insideAny) {
                return false;
            }
        }

        if (rules.hasTimeRange()) {
            int hour = (int) (((world.getTime() / 1000) + 6) % 24);
            boolean inRange = rules.hourMin() <= rules.hourMax()
                    ? hour >= rules.hourMin() && hour <= rules.hourMax()
                    : hour >= rules.hourMin() || hour <= rules.hourMax();
            if (!inRange) {
                return false;
            }
        }

        if (rules.weather() != null && !rules.weather().isBlank()
                && !rules.weather().equalsIgnoreCase("any")) {

            String weather = rules.weather().trim().toLowerCase(Locale.ROOT);
            boolean thundering = world.isThundering();
            boolean raining = world.hasStorm();

            boolean weatherOk = switch (weather) {
                case "clear" -> !raining;
                case "rain" -> raining && !thundering;
                case "thunder" -> thundering;
                default -> true;
            };

            if (!weatherOk) {
                return false;
            }
        }

        if (rules.minDistanceFromPlayers() > 0) {
            boolean tooClose = world.getPlayers().stream()
                    .anyMatch(player -> player.getLocation().distanceSquared(location)
                            < rules.minDistanceFromPlayers() * rules.minDistanceFromPlayers());
            if (tooClose) {
                return false;
            }
        }

        return true;
    }

    private MobDefinition pickWeighted(List<MobDefinition> candidates) {

        double totalWeight = candidates.stream().mapToDouble(def -> def.spawnRules().spawnWeight()).sum();
        double roll = Math.random() * totalWeight;

        for (MobDefinition def : candidates) {
            roll -= def.spawnRules().spawnWeight();
            if (roll <= 0) {
                return def;
            }
        }

        return candidates.get(candidates.size() - 1);
    }

}
