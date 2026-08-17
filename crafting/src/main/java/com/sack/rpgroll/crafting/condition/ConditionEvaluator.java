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
import java.util.UUID;

/**
 * Evalúa las {@link RecipeCondition} de una receta/estación contra un
 * jugador. Cada tipo que depende de otro addon sigue el patrón blando
 * "isReady() -> get()" del ecosistema: si el addon no está instalado, la
 * condición simplemente no se cumple (falla cerrado, no lanza excepción).
 * <p>
 * Las condiciones se dividen en dos grupos: las que solo necesitan el UUID
 * del jugador (LEVEL_MIN/RACE/CLASS/JOB_MIN/GUILD_MEMBER — leen datos
 * persistidos vía UUID, funcionan con el jugador offline) y las que
 * necesitan un {@link Player} en línea de verdad porque dependen de su
 * posición/permisos en ese instante (PERMISSION/BIOME). WORLD/HOUR_RANGE/
 * SEASON/WEATHER usan el {@link World} de la estación como referencia en vez
 * del mundo del jugador — tiene más sentido para una estación fija que para
 * un jugador que puede haberse ido a otro lado.
 */
public class ConditionEvaluator {

    public boolean evaluateAll(List<RecipeCondition> conditions, Player player) {
        return evaluateAllOffline(conditions, player.getUniqueId(), player, player.getWorld());
    }

    public boolean evaluate(RecipeCondition condition, Player player) {
        return evaluateOffline(condition, player.getUniqueId(), player, player.getWorld());
    }

    /**
     * Igual que {@link #evaluateAll(List, Player)} pero sin exigir que el
     * jugador esté en línea — {@code onlinePlayer} puede ser null (se
     * evalúan solo las condiciones que no lo necesitan), y {@code fallbackWorld}
     * reemplaza al mundo del jugador para WORLD/HOUR_RANGE/SEASON/WEATHER.
     */
    public boolean evaluateAllOffline(List<RecipeCondition> conditions, UUID playerId, Player onlinePlayer,
            World fallbackWorld) {

        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (RecipeCondition condition : conditions) {
            if (!evaluateOffline(condition, playerId, onlinePlayer, fallbackWorld)) {
                return false;
            }
        }

        return true;
    }

    public boolean evaluateOffline(RecipeCondition condition, UUID playerId, Player onlinePlayer, World fallbackWorld) {

        return switch (condition.type()) {
            case LEVEL_MIN -> rpgPlayer(playerId).map(p -> p.getLevel() >= condition.minValue()).orElse(false);
            case RACE -> rpgPlayer(playerId).map(p -> condition.value().equals(p.getRace())).orElse(false);
            case CLASS -> rpgPlayer(playerId).map(p -> condition.value().equals(p.getPlayerClass())).orElse(false);
            case JOB_MIN -> rpgPlayer(playerId)
                    .map(p -> p.getJobs().hasJob(condition.value())
                            && p.getJobs().getLevel(condition.value()) >= condition.minValue())
                    .orElse(false);
            case PERMISSION -> onlinePlayer != null && onlinePlayer.hasPermission(condition.value());
            case WORLD -> fallbackWorld != null && fallbackWorld.getName().equalsIgnoreCase(condition.value());
            case HOUR_RANGE -> fallbackWorld != null && evaluateHourRange(fallbackWorld, condition.value());
            case BIOME -> onlinePlayer != null && evaluateBiome(onlinePlayer, condition.value());
            case SEASON -> fallbackWorld != null && evaluateSeason(fallbackWorld, condition.value());
            case GUILD_MEMBER -> evaluateGuildMember(playerId);
            case WEATHER -> fallbackWorld != null && evaluateWeather(fallbackWorld, condition.value());
        };
    }

    private Optional<RPGPlayer> rpgPlayer(UUID playerId) {

        if (playerId == null || !RPGRollAPI.isReady()) {
            return Optional.empty();
        }

        return RPGRollAPI.get().getPlayer(playerId);
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

    private boolean evaluateGuildMember(UUID playerId) {

        if (playerId == null || !GuildsAPI.isReady()) {
            return false;
        }

        return GuildsAPI.getGuildManager().findByMember(playerId).isPresent();
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
