package com.sack.rpgroll.crafting.condition;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.seasons.api.SeasonsAPI;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Evalúa las {@link RecipeCondition} de una receta/estación contra un
 * jugador concreto. Cada tipo que depende de otro addon sigue el patrón
 * blando "isReady() -> get()" del ecosistema: si el addon no está instalado,
 * la condición simplemente no se cumple (falla cerrado, no lanza excepción).
 */
public class ConditionEvaluator {

    public boolean evaluateAll(List<RecipeCondition> conditions, Player player) {

        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (RecipeCondition condition : conditions) {
            if (!evaluate(condition, player)) {
                return false;
            }
        }

        return true;
    }

    public boolean evaluate(RecipeCondition condition, Player player) {

        return switch (condition.type()) {
            case LEVEL_MIN -> rpgPlayer(player).map(p -> p.getLevel() >= condition.minValue()).orElse(false);
            case RACE -> rpgPlayer(player).map(p -> condition.value().equals(p.getRace())).orElse(false);
            case CLASS -> rpgPlayer(player).map(p -> condition.value().equals(p.getPlayerClass())).orElse(false);
            case JOB_MIN -> rpgPlayer(player)
                    .map(p -> p.getJobs().hasJob(condition.value())
                            && p.getJobs().getLevel(condition.value()) >= condition.minValue())
                    .orElse(false);
            case PERMISSION -> player.hasPermission(condition.value());
            case WORLD -> player.getWorld().getName().equalsIgnoreCase(condition.value());
            case HOUR_RANGE -> evaluateHourRange(player.getWorld(), condition.value());
            case BIOME -> evaluateBiome(player, condition.value());
            case SEASON -> evaluateSeason(player.getWorld(), condition.value());
            case GUILD_MEMBER -> evaluateGuildMember(player);
            case WEATHER -> evaluateWeather(player.getWorld(), condition.value());
        };
    }

    private Optional<RPGPlayer> rpgPlayer(Player player) {

        if (!RPGRollAPI.isReady()) {
            return Optional.empty();
        }

        return RPGRollAPI.get().getPlayer(player.getUniqueId());
    }

    private boolean evaluateHourRange(World world, String range) {

        if (range == null || !range.contains("-")) {
            return false;
        }

        String[] parts = range.split("-", 2);
        try {
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());

            long ticks = world.getTime();
            int hour = (int) (((ticks / 1000) + 6) % 24);

            if (start <= end) {
                return hour >= start && hour < end;
            }
            return hour >= start || hour < end;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean evaluateBiome(Player player, String biomeName) {

        if (biomeName == null) {
            return false;
        }

        try {
            Biome required = Biome.valueOf(biomeName.trim().toUpperCase(Locale.ROOT));
            Biome actual = player.getLocation().getBlock().getBiome();
            return actual == required;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean evaluateSeason(World world, String seasonId) {

        if (!SeasonsAPI.isReady()) {
            return false;
        }

        return SeasonsAPI.get().getCurrentSeason(world)
                .map(season -> season.id().equals(seasonId))
                .orElse(false);
    }

    private boolean evaluateGuildMember(Player player) {

        if (!GuildsAPI.isReady()) {
            return false;
        }

        return GuildsAPI.getGuildManager().findByMember(player.getUniqueId()).isPresent();
    }

    private boolean evaluateWeather(World world, String weather) {

        if (weather == null) {
            return false;
        }

        return switch (weather.trim().toUpperCase(Locale.ROOT)) {
            case "THUNDER" -> world.isThundering();
            case "RAIN" -> world.hasStorm() && !world.isThundering();
            case "CLEAR" -> !world.hasStorm();
            default -> false;
        };
    }

}
