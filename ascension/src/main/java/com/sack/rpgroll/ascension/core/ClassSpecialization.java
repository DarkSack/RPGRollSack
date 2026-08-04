package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una rama de especialización de una clase base (ej. Warrior -&gt;
 * Berserker), con su propio árbol de talentos. {@code restrictions} y
 * {@code exclusiveEquipment} son datos informativos — su cumplimiento
 * real depende de otros addons (ej. RPGRoll-Items) que quieran leerlos.
 */
public record ClassSpecialization(
        String id,
        String baseClass,
        String displayName,
        AscensionRequirements requirements,
        Map<String, Double> statBonus,
        List<String> restrictions,
        List<String> exclusiveEquipment,
        List<TalentNode> talentTree) implements RPGContent {

    public ClassSpecialization {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(baseClass, "baseClass no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        requirements = requirements == null ? AscensionRequirements.none() : requirements;
        statBonus = statBonus == null ? Map.of() : Map.copyOf(statBonus);
        restrictions = restrictions == null ? List.of() : List.copyOf(restrictions);
        exclusiveEquipment = exclusiveEquipment == null ? List.of() : List.copyOf(exclusiveEquipment);
        talentTree = talentTree == null ? List.of() : List.copyOf(talentTree);
    }

    public java.util.Optional<TalentNode> talent(String talentId) {
        return talentTree.stream().filter(node -> node.id().equalsIgnoreCase(talentId)).findFirst();
    }

}
