package com.sack.rpgroll.ranching.core.genetics;

import java.util.Objects;

/**
 * Una variante rara asociada a un {@link Gene} — vive DENTRO de la
 * definición del gen (no es un tipo de contenido propio con su propio
 * navegador), porque en la práctica una mutación no tiene sentido sin el
 * gen al que decora.
 *
 * @param chance probabilidad (0.0-1.0) de que esta mutación puntual se dispare al nacer, evaluada
 *               independiente de {@code mutation-chance} global — 0 = usa el valor global de config
 */
public record GeneMutation(String id, String displayName, MutationEffectType effectType, double effectValue,
        double chance) {

    public GeneMutation {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        effectType = effectType == null ? MutationEffectType.COSMETIC_TAG : effectType;
        chance = Math.max(0, Math.min(1, chance));
    }

}
