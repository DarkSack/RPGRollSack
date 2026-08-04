package com.sack.rpgroll.effects.core;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/** Rareza de un efecto — cosmética (color en GUI/lore), no afecta mecánica. */
public enum EffectRarity {

    COMMON(NamedTextColor.WHITE),
    UNCOMMON(NamedTextColor.GREEN),
    RARE(NamedTextColor.AQUA),
    EPIC(NamedTextColor.LIGHT_PURPLE),
    LEGENDARY(NamedTextColor.GOLD);

    private final TextColor color;

    EffectRarity(TextColor color) {
        this.color = color;
    }

    public TextColor color() {
        return color;
    }

}
