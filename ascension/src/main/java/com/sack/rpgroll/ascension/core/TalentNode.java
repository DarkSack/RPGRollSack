package com.sack.rpgroll.ascension.core;

import java.util.List;
import java.util.Map;

/**
 * Un nodo del árbol de talentos de una especialización. Para desbloquearlo
 * hay que haber desbloqueado antes todos sus {@code prerequisites} y pagar
 * {@code cost} puntos de talento.
 */
public record TalentNode(
        String id,
        String displayName,
        int cost,
        List<String> prerequisites,
        Map<String, Double> statBonus,
        String grantedSkill,
        String grantedTrait,
        String grantedEnchantment) {

    public TalentNode {
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
        statBonus = statBonus == null ? Map.of() : Map.copyOf(statBonus);
    }

}
