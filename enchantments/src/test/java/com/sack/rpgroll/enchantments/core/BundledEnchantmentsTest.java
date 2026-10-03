package com.sack.rpgroll.enchantments.core;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los encantamientos que trae el jar: que se lean enteros. El parser descarta
 * en silencio un trigger o un efecto mal escrito, y un encantamiento sin su
 * efecto no avisa de nada en el juego: solo no hace nada.
 */
class BundledEnchantmentsTest {

    private final EnchantmentParser parser = new EnchantmentParser();

    private CustomEnchantment load(String id) throws Exception {

        var stream = getClass().getResourceAsStream("/enchantments/" + id + ".yml");
        assertNotNull(stream, "falta enchantments/" + id + ".yml");

        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return parser.parse(YamlConfiguration.loadConfiguration(reader));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"veinminer", "timber", "autosmelt", "hammer", "tiller", "advanced_mending",
            "advanced_sharpness", "shield_reflect", "shield_parry", "shield_bash", "shield_temper", "shield_aegis"})
    void everyBundledEnchantmentParsesWithTriggersAndEffects(String id) throws Exception {

        CustomEnchantment enchantment = load(id);

        assertEquals(id, enchantment.id());
        assertFalse(enchantment.triggers().isEmpty(), "sin triggers");
        assertFalse(enchantment.categories().isEmpty(), "sin categorías");

        for (int level = 1; level <= enchantment.maxLevel(); level++) {
            int current = level;
            enchantment.effects().forEach(effect -> effect.params().values().stream()
                    .filter(value -> value.startsWith("{") && value.endsWith("}"))
                    .forEach(placeholder -> assertTrue(
                            enchantment.levelData(current).containsKey(placeholder.substring(1, placeholder.length() - 1)),
                            "nivel " + current + " sin " + placeholder)));
        }
    }

    @Test
    void toolEnchantmentsUseTheirNewEffects() throws Exception {
        assertEquals(EnchantEffectType.VEIN_MINE, load("veinminer").effects().get(0).type());
        assertEquals(EnchantEffectType.TREE_FELL, load("timber").effects().get(0).type());
        assertEquals(EnchantEffectType.AUTO_SMELT, load("autosmelt").effects().get(0).type());
        assertEquals(Set.of(Trigger.BLOCK_INTERACT, Trigger.BLOCK_BREAK), load("tiller").triggers());
        assertEquals(Set.of(Trigger.EXP_PICKUP), load("advanced_mending").triggers());
        assertEquals(Set.of(Trigger.SHIELD_DISABLE), load("shield_temper").triggers());
    }

    @Test
    void newCategoriesMatchOnlyTheirTool() {
        assertTrue(EnchantCategory.PICKAXE.matches(Material.DIAMOND_PICKAXE));
        assertFalse(EnchantCategory.AXE.matches(Material.DIAMOND_PICKAXE));
        assertTrue(EnchantCategory.AXE.matches(Material.NETHERITE_AXE));
        assertTrue(EnchantCategory.HOE.matches(Material.IRON_HOE));
        assertTrue(EnchantCategory.SHOVEL.matches(Material.WOODEN_SHOVEL));
        assertTrue(EnchantCategory.SWORD.matches(Material.GOLDEN_SWORD));
        assertFalse(EnchantCategory.SWORD.matches(Material.GOLDEN_AXE));
        assertTrue(EnchantCategory.SHIELD.matches(Material.SHIELD));
        assertFalse(EnchantCategory.SHIELD.matches(Material.IRON_CHESTPLATE));
    }

}
