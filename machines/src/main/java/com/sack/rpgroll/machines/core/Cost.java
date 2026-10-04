package com.sack.rpgroll.machines.core;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Lo que cuesta una mejora: dinero (Vault) y/o ítems.
 * <pre>
 * cost:
 *   money: 500
 *   items:
 *     - "IRON_INGOT:16"                         # material vanilla, sin nombre ni datos
 *     - "rpgroll-items:lingote_de_cobalto:8"    # un ítem de RPGRoll-Items por su id
 * </pre>
 */
public record Cost(double money, List<Item> items) {

    public static final Cost FREE = new Cost(0, List.of());

    /** Un ítem del coste: material vanilla ({@code itemId} null) o ítem de RPGRoll-Items. */
    public record Item(Material material, String itemId, int amount) {

        public boolean custom() {
            return itemId != null;
        }

        /** "8× lingote de cobalto" para la lore cuando no hay nombre mejor. */
        public String label() {
            String name = custom() ? itemId : material.name().toLowerCase(Locale.ROOT);
            return name.replace('_', ' ');
        }
    }

    public boolean free() {
        return money <= 0 && items.isEmpty();
    }

    public static Cost from(ConfigurationSection section, Consumer<String> warn) {

        if (section == null) {
            return FREE;
        }

        List<Item> items = new ArrayList<>();
        for (String raw : section.getStringList("items")) {
            Item item = parseItem(raw);
            if (item == null) {
                warn.accept("coste no válido: '" + raw + "' (usa MATERIAL:cantidad o rpgroll-items:id:cantidad)");
            } else {
                items.add(item);
            }
        }
        return new Cost(Math.max(0, section.getDouble("money", 0)), List.copyOf(items));
    }

    /** {@code IRON_INGOT:16}, {@code IRON_INGOT} (1) o {@code rpgroll-items:lingote_de_cobalto:8}. */
    public static Item parseItem(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split(":");

        if (parts[0].equalsIgnoreCase("rpgroll-items")) {
            if (parts.length < 2 || parts[1].isBlank()) {
                return null;
            }
            Integer amount = parts.length >= 3 ? amount(parts[2]) : Integer.valueOf(1);
            return amount == null ? null : new Item(null, parts[1].toLowerCase(Locale.ROOT), amount);
        }

        Material material = Material.matchMaterial(parts[0]);
        if (material == null || !material.isItem() || material.isAir()) {
            return null;
        }
        Integer amount = parts.length >= 2 ? amount(parts[1]) : Integer.valueOf(1);
        return amount == null ? null : new Item(material, null, amount);
    }

    private static Integer amount(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
