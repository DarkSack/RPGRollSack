package com.sack.rpgroll.ranching.core.breeds;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.common.reskin.EntityReskin;

import java.util.Objects;

/**
 * Una raza siempre pertenece a exactamente una {@code Species} y modifica
 * producción/peso/fertilidad/resistencia como multiplicadores sobre la
 * base de esa especie — la genética individual del animal se aplica
 * encima de esto, no en su lugar. {@code reskin} es el reskin visual
 * propio de esta raza (sin depender de ModelEngine/BetterModel), aplicado
 * vía {@link com.sack.rpgroll.common.reskin.EntityReskinService}.
 */
public record Breed(
        String id,
        String displayName,
        String description,
        String speciesId,
        double productionMultiplier,
        double weightMultiplier,
        double fertilityMultiplier,
        double resistanceMultiplier,
        String temperament,
        EntityReskin reskin) implements RPGContent {

    public Breed {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        Objects.requireNonNull(speciesId, "speciesId no puede ser null");
        productionMultiplier = productionMultiplier <= 0 ? 1.0 : productionMultiplier;
        weightMultiplier = weightMultiplier <= 0 ? 1.0 : weightMultiplier;
        fertilityMultiplier = fertilityMultiplier <= 0 ? 1.0 : fertilityMultiplier;
        resistanceMultiplier = resistanceMultiplier <= 0 ? 1.0 : resistanceMultiplier;
        temperament = temperament == null || temperament.isBlank() ? "Neutral" : temperament;
        reskin = reskin == null ? EntityReskin.NONE : reskin;
    }

}
