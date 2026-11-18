package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.enchantments.core.Rarity;
import com.sack.rpgroll.enchantments.station.EnchantPlan.Cost;
import com.sack.rpgroll.enchantments.station.EnchantPlan.Entry;
import com.sack.rpgroll.enchantments.station.EnchantPlan.Kind;
import com.sack.rpgroll.enchantments.station.EnchantPlan.Prices;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantPlanTest {

    private static Prices prices() {
        Map<Rarity, Integer> levels = new EnumMap<>(Rarity.class);
        Map<Rarity, Integer> lapis = new EnumMap<>(Rarity.class);
        levels.put(Rarity.RARE, 6);
        lapis.put(Rarity.RARE, 10);
        // vanilla: 2 niveles × nivel y 3 lapis por escalón; sobre el máximo: 15 + 10·extra y 16 lapis × extra
        return new Prices(2, 3, 15, 10, 16, levels, lapis);
    }

    private static Entry vanilla(String key, int current, int cap, int max, String... conflicts) {
        return new Entry("minecraft:" + key, Kind.VANILLA, current, cap, max, Rarity.COMMON, Set.of(conflicts), true);
    }

    private static Entry custom(String id, int current, int max, Rarity rarity, String... conflicts) {
        return new Entry("rpgroll:" + id, Kind.CUSTOM, current, max, max, rarity, Set.of(conflicts), true);
    }

    @Test
    void targetIsClampedBetweenZeroAndCap() {
        EnchantPlan plan = new EnchantPlan(List.of(vanilla("sharpness", 2, 8, 5)), prices());
        assertTrue(plan.set("minecraft:sharpness", 12));
        assertEquals(8, plan.target("minecraft:sharpness"));
        assertTrue(plan.set("minecraft:sharpness", -3));
        assertEquals(0, plan.target("minecraft:sharpness"));
    }

    @Test
    void vanillaCostIsCheapUpToMaxAndGrowsAbove() {
        EnchantPlan plan = new EnchantPlan(List.of(vanilla("sharpness", 3, 8, 5)), prices());
        plan.set("minecraft:sharpness", 7);
        // IV: 8+3 · V: 10+3 · VI: 15+16 · VII: 25+32
        assertEquals(new Cost(8 + 10 + 15 + 25, 3 + 3 + 16 + 32), plan.cost());
    }

    @Test
    void customCostDependsOnRarity() {
        EnchantPlan plan = new EnchantPlan(List.of(custom("vampiro", 0, 3, Rarity.RARE)), prices());
        plan.set("rpgroll:vampiro", 2);
        assertEquals(new Cost(6 * 1 + 6 * 2, 10 + 10), plan.cost());
    }

    @Test
    void loweringIsFreeAndShowsAsChange() {
        EnchantPlan plan = new EnchantPlan(List.of(vanilla("unbreaking", 3, 6, 3)), prices());
        plan.set("minecraft:unbreaking", 0);
        assertEquals(new Cost(0, 0), plan.cost());
        assertEquals(Map.of("minecraft:unbreaking", 0), plan.changes());
    }

    @Test
    void conflictBlocksAndRemovingTheOtherFreesIt() {
        EnchantPlan plan = new EnchantPlan(List.of(
                vanilla("sharpness", 5, 8, 5, "minecraft:smite"),
                vanilla("smite", 0, 8, 5, "minecraft:sharpness")), prices());
        assertTrue(plan.blocked("minecraft:smite"));
        assertEquals(List.of("minecraft:sharpness"), plan.blockers("minecraft:smite"));
        assertFalse(plan.set("minecraft:smite", 2));
        assertEquals(0, plan.target("minecraft:smite"));

        plan.set("minecraft:sharpness", 0);
        assertFalse(plan.blocked("minecraft:smite"));
        assertTrue(plan.set("minecraft:smite", 2));
    }

    @Test
    void conflictIsSymmetricEvenIfDeclaredOnOneSide() {
        EnchantPlan plan = new EnchantPlan(List.of(
                custom("hielo", 0, 3, Rarity.RARE, "rpgroll:fuego"),
                custom("fuego", 0, 3, Rarity.RARE)), prices());
        plan.set("rpgroll:fuego", 1);
        assertTrue(plan.blocked("rpgroll:hielo"));
    }

    @Test
    void fixedEntriesBlockButAreNotListed() {
        Entry mending = new Entry("minecraft:mending", Kind.VANILLA, 1, 1, 1, Rarity.COMMON,
                Set.of("minecraft:infinity"), false);
        EnchantPlan plan = new EnchantPlan(List.of(mending, vanilla("infinity", 0, 1, 1)), prices());
        assertEquals(List.of("minecraft:infinity"), plan.editable().stream().map(Entry::id).toList());
        assertTrue(plan.blocked("minecraft:infinity"));
        assertFalse(plan.set("minecraft:mending", 0));
    }

    @Test
    void orderShowsOwnedFirstThenPluginByRarityThenVanillaByName() {
        EnchantPlan plan = new EnchantPlan(List.of(
                vanilla("smite", 0, 8, 5),
                custom("comun", 0, 3, Rarity.UNCOMMON),
                vanilla("looting", 0, 6, 3),
                custom("mitico", 0, 3, Rarity.MYTHIC),
                vanilla("unbreaking", 2, 6, 3)), prices());
        assertEquals(List.of("minecraft:unbreaking", "rpgroll:mitico", "rpgroll:comun", "minecraft:looting",
                "minecraft:smite"), plan.editable().stream().map(Entry::id).toList());
    }

    @Test
    void resetDropsEveryChange() {
        EnchantPlan plan = new EnchantPlan(List.of(vanilla("looting", 1, 6, 3)), prices());
        plan.set("minecraft:looting", 3);
        plan.reset();
        assertEquals(Map.of(), plan.changes());
        assertEquals(1, plan.target("minecraft:looting"));
    }
}
