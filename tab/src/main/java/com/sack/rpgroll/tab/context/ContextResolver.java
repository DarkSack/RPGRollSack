package com.sack.rpgroll.tab.context;

import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

/**
 * Determina qué {@link ContextDefinition} aplica a un jugador en un momento
 * dado: recorre los contextos de mayor a menor prioridad y devuelve el
 * primero cuyas condiciones se cumplan TODAS. No cachea nada — lo llama
 * {@code ProfileManager}/{@code PlayerStateManager} solo cuando algo que
 * pudo cambiar el resultado realmente ocurrió (join, cambio de mundo,
 * cambio de permisos, etc.), nunca por tick.
 */
public class ContextResolver {

    private final ContextManager contextManager;
    private final PlaceholderEngine placeholderEngine;

    public ContextResolver(ContextManager contextManager, PlaceholderEngine placeholderEngine) {
        this.contextManager = contextManager;
        this.placeholderEngine = placeholderEngine;
    }

    public Optional<ContextDefinition> resolve(Player player) {

        for (ContextDefinition context : contextManager.byPriorityDescending()) {
            if (matches(player, context)) {
                return Optional.of(context);
            }
        }

        return Optional.empty();
    }

    private boolean matches(Player player, ContextDefinition context) {

        for (ContextCondition condition : context.conditions()) {
            if (!matches(player, condition)) {
                return false;
            }
        }

        return true;
    }

    private boolean matches(Player player, ContextCondition condition) {

        return switch (condition.type()) {
            case WORLD -> player.getWorld().getName().equalsIgnoreCase(condition.value());
            case PERMISSION -> player.hasPermission(condition.value());
            case GAMEMODE -> player.getGameMode().name().equalsIgnoreCase(condition.value());
            case DIMENSION -> player.getWorld().getEnvironment().name().equalsIgnoreCase(condition.value());
            case WEATHER -> matchesWeather(player, condition.value());
            case PLACEHOLDER -> matchesPlaceholder(player, condition);
        };
    }

    private boolean matchesWeather(Player player, String value) {

        String state = player.getWorld().isThundering() ? "thunder"
                : player.getWorld().hasStorm() ? "rain"
                : "clear";

        return state.equalsIgnoreCase(value);
    }

    private boolean matchesPlaceholder(Player player, ContextCondition condition) {

        String resolved = placeholderEngine.resolve(condition.placeholder(), player);
        String target = condition.value();

        return switch (condition.operator()) {
            case EQUALS -> resolved.equalsIgnoreCase(target);
            case NOT_EQUALS -> !resolved.equalsIgnoreCase(target);
            case CONTAINS -> target != null
                    && resolved.toLowerCase(Locale.ROOT).contains(target.toLowerCase(Locale.ROOT));
            case NOT_EMPTY -> !resolved.isBlank();
            case EMPTY -> resolved.isBlank();
            case GREATER_THAN -> compareNumeric(resolved, target) > 0;
            case LESS_THAN -> compareNumeric(resolved, target) < 0;
            case MATCHES -> target != null && resolved.matches(target);
        };
    }

    private int compareNumeric(String left, String right) {

        try {
            return Double.compare(Double.parseDouble(left.trim()), Double.parseDouble(right.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

}
