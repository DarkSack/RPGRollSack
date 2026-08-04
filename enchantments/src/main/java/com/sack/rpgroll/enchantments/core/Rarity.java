package com.sack.rpgroll.enchantments.core;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Rareza de un encantamiento — puramente cosmética (color en lore/comandos),
 * no afecta ninguna mecánica por sí sola.
 */
public enum Rarity {

    COMMON(NamedTextColor.WHITE),
    UNCOMMON(NamedTextColor.GREEN),
    RARE(NamedTextColor.AQUA),
    EPIC(NamedTextColor.LIGHT_PURPLE),
    LEGENDARY(NamedTextColor.GOLD),
    MYTHIC(NamedTextColor.RED),
    DIVINE(NamedTextColor.YELLOW);

    private final TextColor color;

    Rarity(TextColor color) {
        this.color = color;
    }

    public TextColor color() {
        return color;
    }

}
