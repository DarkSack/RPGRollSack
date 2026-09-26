package com.sack.rpgroll.extras.modifier;

import com.sack.rpgroll.common.character.Characters;
import com.sack.rpgroll.common.character.RPGCharacters;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Resuelve el modificador combinado de un jugador para una clave dada,
 * leyendo su raza/clase/jobs ACTUALES a través de RPGRoll-Lib (sección 18):
 * Core nunca se entera de que Extras existe, y sin Core los modificadores
 * de raza/clase/job simplemente no aplican.
 */
public class ModifierResolver {

    private final ModifierManager modifierManager;
    private final Supplier<Optional<RPGCharacters>> characters;

    public ModifierResolver(ModifierManager modifierManager) {
        this(modifierManager, Characters::get);
    }

    ModifierResolver(ModifierManager modifierManager, Supplier<Optional<RPGCharacters>> characters) {
        this.modifierManager = modifierManager;
        this.characters = characters;
    }

    /** Suma aditiva de todos los modificadores (raza + clase + todos los jobs activos) para esa clave. */
    public double sum(Player player, String key) {

        Optional<RPGCharacters> core = characters.get();

        if (core.isEmpty()) {
            return 0;
        }

        RPGCharacters rp = core.get();
        UUID uuid = player.getUniqueId();
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        double total = 0;

        total += valueFrom(rp.race(uuid).orElse(null), ModifierSourceType.RACE, normalizedKey);
        total += valueFrom(rp.playerClass(uuid).orElse(null), ModifierSourceType.CLASS, normalizedKey);

        for (String jobId : rp.activeJobs(uuid)) {
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

}
