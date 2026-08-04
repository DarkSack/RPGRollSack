package com.sack.rpgroll.fishing.engine;

import com.sack.rpgroll.fishing.core.DepthRequirement;
import com.sack.rpgroll.fishing.core.FishingRegion;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.TimeRequirement;
import com.sack.rpgroll.fishing.core.WaterType;
import com.sack.rpgroll.fishing.core.WeatherType;
import com.sack.rpgroll.fishing.integration.SeasonsIntegration;
import com.sack.rpgroll.seasons.api.SeasonsAPI;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Resuelve todo lo que importa de una picada a partir de la ubicación del
 * anzuelo — bioma/tipo de agua (con override de {@link FishingRegion}),
 * profundidad (por escaneo simple de la columna de agua), clima (vanilla
 * + una temperatura aproximada), hora, y estación (vía RPGRoll-Seasons si
 * está instalado).
 */
public class FishingConditionsResolver {

    private static final int MAX_COLUMN_SCAN = 40;
    private static final int MAX_ROOF_SCAN = 64;

    private final FishingRegionManager regionManager;

    public FishingConditionsResolver(FishingRegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public FishingConditions resolve(Location hookLocation) {

        World world = hookLocation.getWorld();
        Block hookBlock = hookLocation.getBlock();
        Biome biome = hookBlock.getBiome();

        WaterType waterType = resolveWaterType(world.getName(), hookLocation, hookBlock, biome);
        DepthRequirement depth = resolveDepth(hookLocation);
        WeatherType weather = resolveWeather(world, hookLocation);
        Set<TimeRequirement> activeTimes = resolveActiveTimes(world.getTime());
        String seasonId = SeasonsIntegration.currentSeasonId(hookLocation);

        return new FishingConditions(biome.name().toLowerCase(Locale.ROOT), waterType, depth, weather, activeTimes,
                seasonId);
    }

    private WaterType resolveWaterType(String worldName, Location location, Block hookBlock, Biome biome) {

        for (FishingRegion region : regionManager.getAll()) {
            if (region.contains(worldName, location.getX(), location.getY(), location.getZ())) {
                return region.forcedWaterType();
            }
        }

        if (hookBlock.getType() == Material.LAVA) {
            return WaterType.LAVA;
        }

        String name = biome.name();

        if (name.contains("RIVER")) {
            return WaterType.RIVER;
        }

        if (name.contains("SWAMP")) {
            return WaterType.SWAMP;
        }

        if (name.contains("DEEP") && name.contains("OCEAN")) {
            return WaterType.DEEP_OCEAN;
        }

        if (name.contains("OCEAN")) {
            return WaterType.OCEAN;
        }

        return WaterType.LAKE;
    }

    private DepthRequirement resolveDepth(Location hookLocation) {

        World world = hookLocation.getWorld();
        int x = hookLocation.getBlockX();
        int z = hookLocation.getBlockZ();
        int hookY = hookLocation.getBlockY();

        int surfaceY = hookY;
        int surfaceLimit = Math.min(world.getMaxHeight(), hookY + MAX_COLUMN_SCAN);

        while (surfaceY < surfaceLimit && isWater(world.getBlockAt(x, surfaceY + 1, z))) {
            surfaceY++;
        }

        int floorY = hookY;
        int floorLimit = Math.max(world.getMinHeight(), hookY - MAX_COLUMN_SCAN);

        while (floorY > floorLimit && isWater(world.getBlockAt(x, floorY - 1, z))) {
            floorY--;
        }

        if (isRoofed(world, x, z, surfaceY)) {
            return DepthRequirement.UNDERWATER_CAVE;
        }

        if (hookY - floorY <= 1) {
            return DepthRequirement.BOTTOM;
        }

        if (surfaceY - hookY <= 1) {
            return DepthRequirement.SURFACE;
        }

        return DepthRequirement.MID_WATER;
    }

    private boolean isRoofed(World world, int x, int z, int surfaceY) {

        int scanLimit = Math.min(world.getMaxHeight(), surfaceY + MAX_ROOF_SCAN);

        for (int y = surfaceY + 1; y < scanLimit; y++) {
            if (world.getBlockAt(x, y, z).getType().isSolid()) {
                return true;
            }
        }

        return false;
    }

    private boolean isWater(Block block) {
        return block.getType() == Material.WATER;
    }

    private WeatherType resolveWeather(World world, Location location) {

        if (world.isThundering()) {
            return WeatherType.STORM;
        }

        if (world.hasStorm()) {
            return resolveApproxTemperature(location) < 0 ? WeatherType.SNOW : WeatherType.RAIN;
        }

        return WeatherType.SUNNY;
    }

    private double resolveApproxTemperature(Location location) {

        if (SeasonsAPI.isReady()) {
            return SeasonsAPI.get().getTemperature(location);
        }

        String biome = location.getBlock().getBiome().name();

        if (biome.contains("SNOWY") || biome.contains("FROZEN") || biome.contains("ICE") || biome.contains("TAIGA")) {
            return -5;
        }

        return 15;
    }

    private Set<TimeRequirement> resolveActiveTimes(long rawTime) {

        long time = ((rawTime % 24000) + 24000) % 24000;
        Set<TimeRequirement> result = new HashSet<>();

        if (time < 12000) {
            result.add(TimeRequirement.DAY);
        }

        if (time >= 13000 && time < 23000) {
            result.add(TimeRequirement.NIGHT);
        }

        if (time >= 22500 || time < 1000) {
            result.add(TimeRequirement.DAWN);
        }

        if (time >= 12000 && time < 13500) {
            result.add(TimeRequirement.DUSK);
        }

        if (time >= 5500 && time < 6500) {
            result.add(TimeRequirement.NOON);
        }

        if (time >= 17500 && time < 18500) {
            result.add(TimeRequirement.MIDNIGHT);
        }

        return result;
    }

}
