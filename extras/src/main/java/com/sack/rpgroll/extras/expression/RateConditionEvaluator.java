package com.sack.rpgroll.extras.expression;

import com.sack.rpgroll.extras.activity.ActivityStateResolver;

import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Evalúa la condición de una {@code RateRule} (sección 4 y 5): sin prefijo
 * es una palabra clave de actividad ({@code resting}/{@code walking}/
 * {@code sprinting}/{@code combat}); con prefijo {@code biome:}/{@code weather:}/
 * {@code world:}/{@code dimension:} es una condición ambiental (para
 * modificadores de sed por bioma, sección 5). Vacía = siempre verdadero.
 */
public class RateConditionEvaluator {

    private final ActivityStateResolver activityStateResolver;

    public RateConditionEvaluator(ActivityStateResolver activityStateResolver) {
        this.activityStateResolver = activityStateResolver;
    }

    public boolean matches(String condition, Player player) {

        if (condition == null || condition.isBlank()) {
            return true;
        }

        String trimmed = condition.trim();
        int colon = trimmed.indexOf(':');

        if (colon < 0) {

            if (trimmed.equalsIgnoreCase("underwater")) {
                return player.isInWater() && player.getEyeLocation().getBlock().isLiquid();
            }

            return activityStateResolver.resolve(player).name().equalsIgnoreCase(trimmed);
        }

        String key = trimmed.substring(0, colon).toLowerCase(Locale.ROOT);
        String value = trimmed.substring(colon + 1);

        return switch (key) {
            case "biome" -> player.getLocation().getBlock().getBiome().toString().equalsIgnoreCase(value);
            case "weather" -> matchesWeather(player, value);
            case "world" -> player.getWorld().getName().equalsIgnoreCase(value);
            case "dimension" -> player.getWorld().getEnvironment().name().equalsIgnoreCase(value);
            default -> false;
        };
    }

    private boolean matchesWeather(Player player, String value) {

        String state = player.getWorld().isThundering() ? "thunder"
                : player.getWorld().hasStorm() ? "rain"
                : "clear";

        return state.equalsIgnoreCase(value);
    }

}
