package com.sack.rpgroll.quests.core;

import org.bukkit.Material;

public record ItemRequirement(Material material, int amount) {

    public ItemRequirement {
        amount = Math.max(amount, 1);
    }

}
