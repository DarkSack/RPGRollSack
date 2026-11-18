package com.sack.rpgroll.mobs.rarity;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MobRaritiesTest {

    private static MobRarities.Tier tier(String id, double weight) {
        return new MobRarities.Tier(id, Component.text(id), weight, 1, 1, 1, 1, 0, 1, false, false, false);
    }

    @Test
    void picksByWeightInOrder() {
        List<MobRarities.Tier> tiers = List.of(tier("comun", 85), tier("raro", 12), tier("legendario", 2.7), tier("galactico", 0.3));
        assertEquals("comun", MobRarities.pick(tiers, 0.0).id());
        assertEquals("comun", MobRarities.pick(tiers, 0.849).id());
        assertEquals("raro", MobRarities.pick(tiers, 0.851).id());
        assertEquals("legendario", MobRarities.pick(tiers, 0.98).id());
        assertEquals("galactico", MobRarities.pick(tiers, 0.9985).id());
        assertEquals("galactico", MobRarities.pick(tiers, 0.99999999).id());
        assertNull(MobRarities.pick(List.of(tier("x", 0)), 0.5));
    }
}
