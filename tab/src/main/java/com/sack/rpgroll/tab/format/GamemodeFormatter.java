package com.sack.rpgroll.tab.format;

import org.bukkit.GameMode;

import java.util.Map;

/** Formatea el gamemode de un jugador como icono corto ({@code [S]}) o nombre completo, según configuración. */
public final class GamemodeFormatter {

    private static final Map<GameMode, String> SHORT_ICONS = Map.of(
            GameMode.SURVIVAL, "&a[S]",
            GameMode.CREATIVE, "&b[C]",
            GameMode.ADVENTURE, "&e[A]",
            GameMode.SPECTATOR, "&7[SP]");

    private GamemodeFormatter() {
    }

    public static String format(GameMode gameMode, boolean shortIcon) {

        if (shortIcon) {
            return SHORT_ICONS.getOrDefault(gameMode, "&7[?]");
        }

        return switch (gameMode) {
            case SURVIVAL -> "Survival";
            case CREATIVE -> "Creative";
            case ADVENTURE -> "Adventure";
            case SPECTATOR -> "Spectator";
        };
    }

}
