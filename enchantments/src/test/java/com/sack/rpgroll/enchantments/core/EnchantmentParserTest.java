package com.sack.rpgroll.enchantments.core;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentParserTest {

    private final EnchantmentParser parser = new EnchantmentParser();

    private YamlConfiguration configFrom(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @Test
    void parseThrowsWhenIdMissing() {
        YamlConfiguration config = configFrom("display-name: Sharpness");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void parseAppliesDefaultsWhenOptionalBlocksMissing() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                effects:
                  - HEAL
                """);

        CustomEnchantment enchantment = parser.parse(config);

        assertEquals("sharpness", enchantment.displayName());
        assertEquals(Rarity.COMMON, enchantment.rarity());
        assertEquals(1, enchantment.maxLevel());
        assertTrue(enchantment.categories().isEmpty());
        assertTrue(enchantment.allowedItems().isEmpty());
        assertTrue(enchantment.conflicts().isEmpty());
        assertTrue(enchantment.triggers().isEmpty());
        assertEquals(100.0, enchantment.chance());
    }

    @Test
    void parseFallsBackToCommonRarityForInvalidValue() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                rarity: not-a-rarity
                effects:
                  - HEAL
                """);

        assertEquals(Rarity.COMMON, parser.parse(config).rarity());
    }

    @Test
    void parseReadsRarityCaseInsensitively() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                rarity: legendary
                effects:
                  - HEAL
                """);

        assertEquals(Rarity.LEGENDARY, parser.parse(config).rarity());
    }

    @Test
    void parseIgnoresInvalidMaterialsAndCategories() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                categories:
                  - WEAPON
                  - not-a-category
                allowed-items:
                  - DIAMOND_SWORD
                  - NOT_A_MATERIAL
                effects:
                  - HEAL
                """);

        CustomEnchantment enchantment = parser.parse(config);

        assertEquals(1, enchantment.categories().size());
        assertTrue(enchantment.categories().contains(EnchantCategory.WEAPON));
        assertEquals(1, enchantment.allowedItems().size());
        assertTrue(enchantment.allowedItems().contains(Material.DIAMOND_SWORD));
    }

    @Test
    void parseUppercasesConflicts() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                conflicts:
                  - smite
                effects:
                  - HEAL
                """);

        assertTrue(parser.parse(config).conflicts().contains("SMITE"));
    }

    @Test
    void parseIgnoresInvalidTriggers() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                trigger:
                  - PLAYER_ATTACK
                  - not-a-trigger
                effects:
                  - HEAL
                """);

        assertEquals(1, parser.parse(config).triggers().size());
        assertTrue(parser.parse(config).triggers().contains(Trigger.PLAYER_ATTACK));
    }

    @Test
    void parseChanceAcceptsPercentSuffix() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                chance: 25%
                effects:
                  - HEAL
                """);

        assertEquals(25.0, parser.parse(config).chance());
    }

    @Test
    void parseChanceAcceptsPlainNumber() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                chance: 40
                effects:
                  - HEAL
                """);

        assertEquals(40.0, parser.parse(config).chance());
    }

    @Test
    void parseChanceFallsBackTo100OnInvalidValue() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                chance: not-a-number
                effects:
                  - HEAL
                """);

        assertEquals(100.0, parser.parse(config).chance());
    }

    @Test
    void parseLevelsCollectsNumericDataPerLevelAndSkipsInvalidKeys() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                max-level: 3
                levels:
                  '1':
                    damage: 2
                  '2':
                    damage: 4.5
                  not-a-number:
                    damage: 99
                effects:
                  - HEAL
                """);

        CustomEnchantment enchantment = parser.parse(config);

        assertEquals(2.0, enchantment.levelData(1).get("damage"));
        assertEquals(4.5, enchantment.levelData(2).get("damage"));
        assertTrue(enchantment.levelData(99).isEmpty());
    }

    @Test
    void parseSimpleStringEffectsResolveByEffectTypeName() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                effects:
                  - DAMAGE
                  - not-a-real-effect
                """);

        CustomEnchantment enchantment = parser.parse(config);

        assertEquals(1, enchantment.effects().size());
        assertEquals(EnchantEffectType.DAMAGE, enchantment.effects().get(0).type());
    }

    @Test
    void parseDetailedEffectsReadTypeAndParams() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                effects:
                  - type: DAMAGE
                    amount: 5
                """);

        CustomEnchantment enchantment = parser.parse(config);

        EnchantEffect effect = enchantment.effects().get(0);
        assertEquals(EnchantEffectType.DAMAGE, effect.type());
        assertEquals("5", effect.param("amount", "0"));
    }

    @Test
    void parseDetailedEffectSkippedWhenTypeMissingOrInvalid() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                effects:
                  - amount: 5
                  - type: not-a-type
                    amount: 5
                  - DAMAGE
                """);

        CustomEnchantment enchantment = parser.parse(config);

        assertEquals(1, enchantment.effects().size());
    }

    @Test
    void parseThrowsWhenNoEffectsResolve() {
        YamlConfiguration config = configFrom("""
                id: sharpness
                effects:
                  - not-a-real-effect
                """);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }
}
