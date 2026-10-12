package com.sack.rpgroll.extras.activity;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;

/**
 * Decide si un jugador está AFK para congelar sus stats: mientras lo está,
 * ni el decay ni la regeneración periódica los tocan (la sed no baja, la
 * fatiga no sube). Usa el tiempo de inactividad de Paper, que se reinicia con
 * cualquier acción real del jugador (moverse, girar la cámara, chatear,
 * golpear...) pero no cuando lo arrastra el agua o lo empuja otra entidad.
 */
public record AfkPolicy(boolean pauseStats, Duration idleThreshold, Set<String> offWorlds) {

    public static final AfkPolicy DISABLED = new AfkPolicy(false, Duration.ZERO, Set.of());

    private static final int DEFAULT_IDLE_SECONDS = 300;

    /** Lee la sección {@code afk} de config.yml; sin ella, pausa tras 5 minutos. */
    public static AfkPolicy from(ConfigurationSection config) {

        ConfigurationSection afk = config == null ? null : config.getConfigurationSection("afk");
        Set<String> offWorlds = config == null ? Set.of() : Set.copyOf(config.getStringList("stats-disabled-worlds"));

        if (afk == null) {
            return new AfkPolicy(true, Duration.ofSeconds(DEFAULT_IDLE_SECONDS), offWorlds);
        }

        int seconds = Math.max(1, afk.getInt("idle-seconds", DEFAULT_IDLE_SECONDS));
        return new AfkPolicy(afk.getBoolean("pause-stats", true), Duration.ofSeconds(seconds), offWorlds);
    }

    public boolean freezes(Player player) {
        return inOffWorld(player) || (pauseStats && player.getIdleDuration().compareTo(idleThreshold) >= 0);
    }

    /** En los mundos de {@code stats-disabled-worlds} (un lobby) las necesidades ni corren ni hacen daño. */
    public boolean inOffWorld(Player player) {
        return !offWorlds.isEmpty() && offWorlds.contains(player.getWorld().getName());
    }

}
