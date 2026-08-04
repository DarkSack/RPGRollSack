package com.sack.rpgroll.items.core;

import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Define un ítem RPGRoll completo. Cada componente descrito en la
 * filosofía del addon (Display, Stats, Attributes, Requirements,
 * Durability, Enchantments, Effects, Triggers/Actions, Abilities, Sockets,
 * Skins, Upgrades, Recipes, Economy, Custom Data) es un campo
 * independiente — "deshabilitado" simplemente significa lista/mapa vacío
 * o 0.
 */
public record ItemDefinition(
        String id,
        String category,
        Material material,
        String displayName,
        List<String> lore,
        Integer customModelData,
        String rarityId,
        Boolean glowOverride,
        List<String> flags,
        boolean unbreakable,
        String dyeColor,
        String skullTexture,
        ArmorTrimDef trim,
        Map<String, Double> stats,
        Map<String, Double> attributeModifiers,
        ItemRequirements requirements,
        ItemDurabilityDef durability,
        Map<String, Integer> vanillaEnchantments,
        Map<String, Integer> customEnchantments,
        List<ItemEffectDef> effects,
        Map<ItemTrigger, List<ItemAction>> triggers,
        List<ItemAbility> abilities,
        List<SocketDefinition> sockets,
        List<ItemSkin> skins,
        List<UpgradeLevel> upgrades,
        List<ItemRecipeDef> recipes,
        double sellPrice,
        double buyPrice,
        Map<String, String> customData) implements RPGContent {

    public ItemDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(material, "material no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        category = category == null || category.isBlank() ? "misc" : category;
        displayName = displayName == null ? id : displayName;
        lore = lore == null ? List.of() : List.copyOf(lore);
        rarityId = rarityId == null || rarityId.isBlank() ? "common" : rarityId;
        flags = flags == null ? List.of() : List.copyOf(flags);
        stats = stats == null ? Map.of() : Map.copyOf(stats);
        attributeModifiers = attributeModifiers == null ? Map.of() : Map.copyOf(attributeModifiers);
        requirements = requirements == null ? ItemRequirements.none() : requirements;
        durability = durability == null ? ItemDurabilityDef.disabled() : durability;
        vanillaEnchantments = vanillaEnchantments == null ? Map.of() : Map.copyOf(vanillaEnchantments);
        customEnchantments = customEnchantments == null ? Map.of() : Map.copyOf(customEnchantments);
        effects = effects == null ? List.of() : List.copyOf(effects);
        triggers = triggers == null ? Map.of() : Map.copyOf(triggers);
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
        sockets = sockets == null ? List.of() : List.copyOf(sockets);
        skins = skins == null ? List.of() : List.copyOf(skins);
        upgrades = upgrades == null ? List.of() : List.copyOf(upgrades);
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
        customData = customData == null ? Map.of() : Map.copyOf(customData);
    }

    public List<ItemAction> actionsFor(ItemTrigger trigger) {
        return triggers.getOrDefault(trigger, List.of());
    }

    public double stat(String key) {
        return stats.getOrDefault(key, 0.0);
    }

}
