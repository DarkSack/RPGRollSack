package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Representa un trabajo del sistema RPG (Minero, Pescador, Cazador, etc.).
 * Objeto de datos inmutable — no contiene lógica de progreso del jugador
 * (eso vive en PlayerJobs) ni de disparo de recompensas (eso vive en
 * los listeners específicos + JobRewardService).
 *
 * @param expBase       XP base usada en la fórmula de curva de nivel
 * @param expMultiplier multiplicador exponencial de la curva de nivel
 * @param rewards       target (ej. nombre de Material/EntityType) -> recompensa
 */
public record Job(
        String id,
        String displayName,
        String description,
        String icon,
        List<String> lore,
        int maxLevel,
        int expBase,
        double expMultiplier,
        Map<String, JobReward> rewards
) implements RPGContent {

    public Job {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        if (maxLevel <= 0) {
            throw new IllegalArgumentException("maxLevel debe ser mayor a 0");
        }

        description = description == null ? "" : description;
        icon = icon == null ? "" : icon;
        lore = lore == null ? List.of() : List.copyOf(lore);
        rewards = rewards == null ? Map.of() : Map.copyOf(rewards);
    }

    /**
     * Calcula la experiencia total requerida para alcanzar un nivel dado.
     * Misma fórmula usada en LevelUpRewardsConfig: expBase * (nivel-1)^multiplicador.
     */
    public int getExpRequiredForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        return (int) (expBase * Math.pow(level - 1, expMultiplier));
    }

    /**
     * Busca la recompensa configurada para un target específico
     * (ej. nombre de Material "DIAMOND_ORE", o EntityType "ZOMBIE").
     */
    public JobReward getReward(String target) {
        return rewards.get(target);
    }

    public boolean hasReward(String target) {
        return rewards.containsKey(target);
    }

}