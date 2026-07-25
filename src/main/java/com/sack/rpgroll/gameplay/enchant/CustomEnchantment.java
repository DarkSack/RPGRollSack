package com.sack.rpgroll.gameplay.enchant;

import com.sack.rpgroll.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record CustomEnchantment(
        String id,
        String displayName,
        String description,
        List<String> lore,
        int maxLevel,
        Set<ItemCategory> applicableTo,
        EnchantTrigger trigger,
        String effectType,
        Map<String, Object> params,
        double dropChance,
        Set<String> dropMobs,
        double shopPriceBase,
        double shopPricePerLevel) implements RPGContent {

    public CustomEnchantment {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");
        Objects.requireNonNull(trigger, "trigger no puede ser null");
        Objects.requireNonNull(effectType, "effectType no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }
        if (maxLevel <= 0) {
            throw new IllegalArgumentException("maxLevel debe ser mayor a 0");
        }
        if (applicableTo == null || applicableTo.isEmpty()) {
            throw new IllegalArgumentException("applicableTo no puede estar vacío");
        }

        description = description == null ? "" : description;
        lore = lore == null ? List.of() : List.copyOf(lore);
        applicableTo = Set.copyOf(applicableTo);
        params = params == null ? Map.of() : Map.copyOf(params);
        dropMobs = dropMobs == null ? Set.of() : Set.copyOf(dropMobs);
        dropChance = Math.max(0, dropChance);
        shopPriceBase = Math.max(0, shopPriceBase);
        shopPricePerLevel = Math.max(0, shopPricePerLevel);
    }

    public double getParamDouble(String key, double defaultValue) {
        Object value = params.get(key);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public int getParamInt(String key, int defaultValue) {
        Object value = params.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    public String getParamString(String key, String defaultValue) {
        Object value = params.get(key);
        return value instanceof String string ? string : defaultValue;
    }

    public boolean isDroppable() {
        return dropChance > 0 && !dropMobs.isEmpty();
    }

    public boolean isSellable() {
        return shopPriceBase > 0 || shopPricePerLevel > 0;
    }

    public double getShopPrice(int level) {
        return shopPriceBase + (shopPricePerLevel * level);
    }

}