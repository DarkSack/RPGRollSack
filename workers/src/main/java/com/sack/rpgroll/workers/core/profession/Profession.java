package com.sack.rpgroll.workers.core.profession;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.workers.core.economy.WageType;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Una profesión — identidad, qué habilidades aplican, sus reglas de IA
 * (ver {@link AiRule}), su horario, su salario, y opcionalmente qué
 * herramienta necesita para rendir al máximo. El comportamiento físico
 * de "trabajar" (qué bloques rompe, qué recoge) NO vive acá — lo resuelve
 * un {@code ProfessionBehavior} registrado por id (built-in para minero/
 * granjero/leñador/ganadero/pescador, o registrado por un addon vía
 * {@code WorkersAPI.registerBehavior}).
 *
 * @param entityType      nombre de un {@link org.bukkit.entity.EntityType} vanilla usado para spawnear al worker
 * @param skillIds        ids de {@link com.sack.rpgroll.workers.core.skill.Skill} que aplican a esta profesión
 * @param aiRules         reglas condición→acción, evaluadas en orden de {@code priority}
 * @param scheduleId      id de un {@link com.sack.rpgroll.workers.core.schedule.Schedule}, o null = sin horario fijo
 * @param toolMaterial    material de herramienta requerido para rendimiento completo, o null = no requiere ninguna
 */
public record Profession(
        String id,
        String displayName,
        String icon,
        String description,
        String entityType,
        Set<String> skillIds,
        List<AiRule> aiRules,
        String scheduleId,
        double wageAmount,
        WageType wageType,
        String toolMaterial) implements RPGContent {

    public Profession {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "VILLAGER_SPAWN_EGG" : icon;
        description = description == null ? "" : description;
        entityType = entityType == null || entityType.isBlank() ? "VILLAGER" : entityType.toUpperCase(java.util.Locale.ROOT);
        skillIds = skillIds == null ? Set.of() : Set.copyOf(skillIds);
        aiRules = aiRules == null ? List.of() : List.copyOf(aiRules);
        scheduleId = scheduleId == null || scheduleId.isBlank() ? null : scheduleId;
        wageAmount = Math.max(0, wageAmount);
        wageType = wageType == null ? WageType.PER_TASK : wageType;
        toolMaterial = toolMaterial == null || toolMaterial.isBlank() ? null : toolMaterial.toUpperCase(java.util.Locale.ROOT);
    }

    /** Reglas ya ordenadas por prioridad ascendente — la primera que matchee gana. */
    public List<AiRule> orderedRules() {
        return aiRules.stream().sorted(java.util.Comparator.comparingInt(AiRule::priority)).toList();
    }

}
