package com.sack.rpgroll.workers.core.skill;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Una habilidad — siempre pertenece a UNA profesión (a diferencia de los
 * genes de RPGRoll-Ranching, que pueden ser genéricos). Un worker sube de
 * nivel esta habilidad con experiencia; cada nivel suma
 * {@code valuePerLevel} al atributo {@code attributeKey}, que el
 * {@code ProfessionBehavior} correspondiente consulta por nombre (ej.
 * "mining_speed", "catch_rarity", "crop_quality").
 */
public record Skill(String id, String displayName, String description, String professionId, int maxLevel,
        String attributeKey, double valuePerLevel) implements RPGContent {

    public Skill {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        Objects.requireNonNull(professionId, "professionId no puede ser null");
        maxLevel = Math.max(1, maxLevel);
        Objects.requireNonNull(attributeKey, "attributeKey no puede ser null");
        valuePerLevel = Math.max(0, valuePerLevel);
    }

}
