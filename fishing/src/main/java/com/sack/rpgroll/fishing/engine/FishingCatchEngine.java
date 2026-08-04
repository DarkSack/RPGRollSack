package com.sack.rpgroll.fishing.engine;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.fishing.core.Bait;
import com.sack.rpgroll.fishing.core.CatchQuality;
import com.sack.rpgroll.fishing.core.FishRarity;
import com.sack.rpgroll.fishing.core.FishSpecies;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.core.Junk;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.fishing.core.Treasure;
import com.sack.rpgroll.fishing.core.TreasureManager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Decide qué salió de una picada: tesoro, basura, o una especie de pez
 * (con peso/longitud/calidad/precio/experiencia ya sorteados) — dado el
 * lugar/momento ({@link FishingConditions}), la caña y la carnada usadas.
 */
public class FishingCatchEngine {

    private final FishSpeciesManager speciesManager;
    private final TreasureManager treasureManager;
    private final JunkManager junkManager;
    private final FishingConditionsResolver conditionsResolver;
    private final double treasureChance;
    private final double junkChance;
    private final Random random = new Random();

    public FishingCatchEngine(FishSpeciesManager speciesManager, TreasureManager treasureManager,
            JunkManager junkManager, FishingConditionsResolver conditionsResolver, double treasureChance,
            double junkChance) {
        this.speciesManager = speciesManager;
        this.treasureManager = treasureManager;
        this.junkManager = junkManager;
        this.conditionsResolver = conditionsResolver;
        this.treasureChance = treasureChance;
        this.junkChance = junkChance;
    }

    public CatchResult resolveCatch(Player player, Location hookLocation, FishingRod rod, Bait bait) {

        double roll = random.nextDouble();

        if (roll < treasureChance) {
            return rollTreasure();
        }

        if (roll < treasureChance + junkChance) {
            return rollJunk();
        }

        FishingConditions conditions = conditionsResolver.resolve(hookLocation);

        List<FishSpecies> eligible = speciesManager.getAll().stream()
                .filter(species -> isEligible(species, conditions, player, bait))
                .toList();

        if (eligible.isEmpty()) {
            return rollJunk();
        }

        FishSpecies chosen = weightedPick(eligible, rod, bait);
        double weight = randomInRange(chosen.minWeight(), chosen.maxWeight());
        double length = randomInRange(chosen.minLength(), chosen.maxLength());
        CatchQuality quality = rollQuality(rod, bait, player);

        double weightFactor = 0.5 + (chosen.maxWeight() > chosen.minWeight()
                ? (weight - chosen.minWeight()) / (chosen.maxWeight() - chosen.minWeight())
                : 0.5);

        double price = chosen.basePrice() * weightFactor * quality.priceMultiplier();
        int experience = (int) Math.round(chosen.baseExperience() * quality.priceMultiplier());

        return CatchResult.fish(chosen, weight, length, quality, price, experience);
    }

    private boolean isEligible(FishSpecies species, FishingConditions conditions, Player player, Bait bait) {

        if (!species.waterTypes().isEmpty() && !species.waterTypes().contains(conditions.waterType())) {
            return false;
        }

        if (!species.biomes().isEmpty() && !species.biomes().contains(conditions.biome())) {
            return false;
        }

        if (!species.depths().isEmpty() && !species.depths().contains(conditions.depth())) {
            return false;
        }

        if (!species.allowedWeathers().isEmpty() && !species.allowedWeathers().contains(conditions.weather())) {
            return false;
        }

        if (!species.allowedTimes().isEmpty() && Collections.disjoint(species.allowedTimes(), conditions.activeTimes())) {
            return false;
        }

        if (!species.allowedSeasons().isEmpty()
                && (conditions.seasonId() == null || !species.allowedSeasons().contains(conditions.seasonId()))) {
            return false;
        }

        if (species.legendary()) {

            if (RPGRollAPI.isReady()) {
                int level = RPGRollAPI.get().getPlayer(player.getUniqueId()).map(rp -> rp.getLevel()).orElse(0);
                if (level < species.requiredLevel()) {
                    return false;
                }
            } else if (species.requiredLevel() > 0) {
                return false;
            }

            if (species.requiresFullMoon() && !isFullMoon(player.getWorld())) {
                return false;
            }

            if (species.hasRequiredBait() && (bait == null || !bait.id().equals(species.requiredBaitId()))) {
                return false;
            }
        }

        return true;
    }

    private FishSpecies weightedPick(List<FishSpecies> eligible, FishingRod rod, Bait bait) {

        Map<FishSpecies, Double> weights = new LinkedHashMap<>();
        double total = 0;

        for (FishSpecies species : eligible) {
            double weight = computeWeight(species, rod, bait);
            weights.put(species, weight);
            total += weight;
        }

        double pick = random.nextDouble() * total;
        double cumulative = 0;

        for (var entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (pick <= cumulative) {
                return entry.getKey();
            }
        }

        return eligible.get(eligible.size() - 1);
    }

    private double computeWeight(FishSpecies species, FishingRod rod, Bait bait) {

        double weight = species.rarity().baseWeight();

        if (rod != null) {

            if (species.rarity() != FishRarity.COMMON) {
                weight *= rod.luckBonus();
            }

            if (rod.preferredCategories().contains(species.category().name().toLowerCase(java.util.Locale.ROOT))) {
                weight *= 1.5;
            }
        }

        if (bait != null) {

            if (!species.attractedByBaitTags().isEmpty()) {
                boolean matches = !Collections.disjoint(bait.tags(), species.attractedByBaitTags());
                weight *= matches ? 2.0 : 0.3;
            }

            if (species.legendary()) {
                weight *= bait.legendaryWeightMultiplier();
            }
        }

        return Math.max(0.0001, weight);
    }

    private CatchQuality rollQuality(FishingRod rod, Bait bait, Player player) {

        double score = random.nextDouble() * 100;

        if (rod != null) {
            score += rod.precision() * 10;
        }

        if (bait != null) {
            score += bait.qualityBonus() * 10;
        }

        if (RPGRollAPI.isReady()) {
            int level = RPGRollAPI.get().getPlayer(player.getUniqueId()).map(rp -> rp.getLevel()).orElse(0);
            score += level * 0.5;
        }

        if (score >= 95) {
            return CatchQuality.MASTERWORK;
        }

        if (score >= 80) {
            return CatchQuality.PERFECT;
        }

        if (score >= 60) {
            return CatchQuality.EXCELLENT;
        }

        if (score >= 35) {
            return CatchQuality.GOOD;
        }

        return CatchQuality.COMMON;
    }

    private boolean isFullMoon(World world) {
        return (world.getFullTime() / 24000L) % 8 == 0;
    }

    private double randomInRange(double min, double max) {
        return min + random.nextDouble() * Math.max(0, max - min);
    }

    private CatchResult rollTreasure() {

        List<Treasure> treasures = List.copyOf(treasureManager.getAll());

        if (treasures.isEmpty()) {
            return rollJunk();
        }

        return CatchResult.treasure(weightedPickGeneric(treasures, Treasure::weight));
    }

    private CatchResult rollJunk() {

        List<Junk> junks = List.copyOf(junkManager.getAll());

        if (junks.isEmpty()) {
            return CatchResult.nothing();
        }

        return CatchResult.junk(weightedPickGeneric(junks, Junk::weight));
    }

    private <T> T weightedPickGeneric(List<T> items, java.util.function.ToDoubleFunction<T> weightFn) {

        double total = items.stream().mapToDouble(weightFn).sum();
        double pick = random.nextDouble() * total;
        double cumulative = 0;

        for (T item : items) {
            cumulative += weightFn.applyAsDouble(item);
            if (pick <= cumulative) {
                return item;
            }
        }

        return items.get(items.size() - 1);
    }

}
