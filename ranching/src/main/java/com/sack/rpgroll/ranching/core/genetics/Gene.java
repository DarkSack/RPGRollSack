package com.sack.rpgroll.ranching.core.genetics;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Definición de un rasgo hereditario — NO es el valor de un animal
 * puntual, es la plantilla que dice qué atributo modifica, cómo dominan
 * sus dos alelos, y en qué rango de porcentaje se mueve.
 *
 * @param attributeKey    clave libre que un {@link com.sack.rpgroll.ranching.core.production.ProductionEngine},
 *                        el motor de fertilidad, etc. buscan por nombre (ej. "milk_production",
 *                        "growth_speed", "fertility", "meat_quality", "disease_resistance",
 *                        "longevity", "temperament", "feed_consumption") — addons pueden inventar
 *                        claves propias sin tocar código de Ranching.
 * @param applicableSpecies ids de {@link com.sack.rpgroll.ranching.core.species.Species} donde este gen
 *                          se sortea al nacer — vacío = aplica a cualquier especie.
 */
public record Gene(
        String id,
        String displayName,
        String description,
        String attributeKey,
        GeneDominance dominance,
        double minValue,
        double maxValue,
        Set<String> applicableSpecies,
        List<GeneMutation> mutations) implements RPGContent {

    public Gene {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        Objects.requireNonNull(attributeKey, "attributeKey no puede ser null");
        dominance = dominance == null ? GeneDominance.MIXED : dominance;
        minValue = Math.max(0, minValue);
        maxValue = Math.max(minValue, maxValue);
        applicableSpecies = applicableSpecies == null ? Set.of() : Set.copyOf(applicableSpecies);
        mutations = mutations == null ? List.of() : List.copyOf(mutations);
    }

    public boolean appliesTo(String speciesId) {
        return applicableSpecies.isEmpty() || applicableSpecies.contains(speciesId);
    }

    public double clamp(double value) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    public double randomStarting(java.util.Random random) {
        return minValue + random.nextDouble() * (maxValue - minValue);
    }

}
