package com.sack.rpgroll.items.socket;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Map;

/**
 * Una gema/runa/cristal insertable en un {@link com.sack.rpgroll.items.core.SocketDefinition}
 * compatible. Modifica los stats del ítem que la contiene mientras esté
 * insertada.
 */
public record Gem(String id, String displayName, String type, Map<String, Double> statBonus) implements RPGContent {

    public Gem {
        statBonus = statBonus == null ? Map.of() : Map.copyOf(statBonus);
    }

}
