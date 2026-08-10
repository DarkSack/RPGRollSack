package com.sack.rpgroll.extras.modifier;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

/**
 * Resuelve el modificador combinado de un jugador para una clave dada,
 * leyendo su raza/clase/jobs ACTUALES vía la API pública de RPGRoll-Core
 * (sección 18) — Core nunca se entera de que Extras existe.
 */
public class ModifierResolver {

    private final ModifierManager modifierManager;

    public ModifierResolver(ModifierManager modifierManager) {
        this.modifierManager = modifierManager;
    }

    /** Suma aditiva de todos los modificadores (raza + clase + todos los jobs activos) para esa clave. */
    public double sum(Player player, String key) {

        Optional<RPGPlayer> rpgPlayer = resolveRpgPlayer(player);

        if (rpgPlayer.isEmpty()) {
            return 0;
        }

        RPGPlayer rp = rpgPlayer.get();
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        double total = 0;

        total += valueFrom(rp.getRace(), ModifierSourceType.RACE, normalizedKey);
        total += valueFrom(rp.getPlayerClass(), ModifierSourceType.CLASS, normalizedKey);

        for (String jobId : rp.getJobs().getActiveJobIds()) {
            total += valueFrom(jobId, ModifierSourceType.JOB, normalizedKey);
        }

        return total;
    }

    /** Multiplicador combinado (1.0 = sin cambio) — para claves tipo {@code stat_max}/{@code stat_rate}. */
    public double multiplier(Player player, String key) {
        return 1.0 + sum(player, key);
    }

    private double valueFrom(String sourceId, ModifierSourceType expectedType, String key) {

        if (sourceId == null) {
            return 0;
        }

        return modifierManager.get(sourceId)
                .filter(set -> set.type() == expectedType)
                .map(set -> set.values().getOrDefault(key, 0.0))
                .orElse(0.0);
    }

    private Optional<RPGPlayer> resolveRpgPlayer(Player player) {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            return Optional.empty();
        }

        PlayerManager playerManager = rpgRoll.getBootstrap().getServices().get(PlayerManager.class);

        if (playerManager == null) {
            return Optional.empty();
        }

        return playerManager.getPlayer(player.getUniqueId());
    }

}
