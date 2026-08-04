package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Un tesoro pescable — cofres, reliquias, llaves, botellas, mapas... Por
 * ahora entrega un ItemStack vanilla puro (material + cantidad); referenciar
 * un ítem custom de RPGRoll-Items queda para una integración futura.
 *
 * @param weight peso relativo entre tesoros al sortear cuál salió, dado que ya se decidió que la picada fue tesoro
 */
public record Treasure(String id, String displayName, String icon, String description, FishRarity rarity,
        String rewardMaterial, int rewardAmount, double weight) implements RPGContent {

    public Treasure {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "CHEST" : icon;
        description = description == null ? "" : description;
        rarity = rarity == null ? FishRarity.UNCOMMON : rarity;
        rewardMaterial = rewardMaterial == null || rewardMaterial.isBlank() ? "CHEST" : rewardMaterial;
        rewardAmount = Math.max(1, rewardAmount);
        weight = Math.max(0.01, weight);
    }

}
