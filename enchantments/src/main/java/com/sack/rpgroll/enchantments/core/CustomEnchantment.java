package com.sack.rpgroll.enchantments.core;

import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Define un encantamiento personalizado completo: identidad, restricciones
 * de aplicación (rareza, categorías/materiales permitidos, conflictos,
 * niveles) y comportamiento (triggers, condiciones, probabilidad, efectos).
 * Puede provenir de un YAML en enchantments/ o registrarse por código vía
 * {@link EnchantmentManager#register(CustomEnchantment)}.
 */
public record CustomEnchantment(
        String id,
        String displayName,
        Rarity rarity,
        Set<EnchantCategory> categories,
        List<Material> allowedItems,
        int maxLevel,
        Map<Integer, Map<String, Double>> levelData,
        List<String> conflicts,
        Set<Trigger> triggers,
        List<String> conditions,
        double chance,
        List<EnchantEffect> effects) implements RPGContent {

    public CustomEnchantment {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        if (maxLevel < 1) {
            throw new IllegalArgumentException("el encantamiento '" + id + "' necesita max-level >= 1");
        }

        if (effects == null || effects.isEmpty()) {
            throw new IllegalArgumentException("el encantamiento '" + id + "' necesita al menos un efecto");
        }

        rarity = rarity == null ? Rarity.COMMON : rarity;
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        allowedItems = allowedItems == null ? List.of() : List.copyOf(allowedItems);
        levelData = levelData == null ? Map.of() : Map.copyOf(levelData);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        triggers = triggers == null ? Set.of() : Set.copyOf(triggers);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        effects = List.copyOf(effects);
        chance = chance <= 0 ? 100.0 : chance;
    }

    /** Datos numéricos definidos en "levels.&lt;nivel&gt;" para el nivel dado, o vacío si no hay. */
    public Map<String, Double> levelData(int level) {
        return levelData.getOrDefault(level, Map.of());
    }

}
