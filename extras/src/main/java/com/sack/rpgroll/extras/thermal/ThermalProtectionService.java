package com.sack.rpgroll.extras.thermal;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Suma la protección térmica de toda la armadura equipada (sección 8): lee
 * {@code thermal_insulation}/{@code thermal_cold_resistance}/{@code thermal_heat_resistance}
 * del custom-data genérico de un ítem de RPGRoll-Items si existe; si no,
 * cae a una tabla de materiales vanilla razonable.
 */
public class ThermalProtectionService {

    public ThermalProperties netProtection(Player player) {

        PlayerInventory inventory = player.getInventory();
        ThermalProperties total = ThermalProperties.NONE;

        for (ItemStack piece : new ItemStack[] {
                inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots() }) {

            if (piece == null || piece.getType().isAir()) {
                continue;
            }

            total = total.combine(resolve(piece));
        }

        return total;
    }

    private ThermalProperties resolve(ItemStack item) {

        Map<String, String> customData = ItemCustomDataReader.read(item);

        if (!customData.isEmpty() && hasThermalKeys(customData)) {
            return new ThermalProperties(
                    parseOrZero(customData.get("thermal_insulation")),
                    parseOrZero(customData.get("thermal_cold_resistance")),
                    parseOrZero(customData.get("thermal_heat_resistance")));
        }

        return vanillaFallback(item.getType());
    }

    private boolean hasThermalKeys(Map<String, String> customData) {
        return customData.containsKey("thermal_insulation") || customData.containsKey("thermal_cold_resistance")
                || customData.containsKey("thermal_heat_resistance");
    }

    private double parseOrZero(String raw) {

        if (raw == null) {
            return 0;
        }

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Tabla de fallback para armadura vanilla sin custom-data — valores conservadores por familia de material. */
    private ThermalProperties vanillaFallback(Material material) {

        String name = material.name();

        if (name.startsWith("LEATHER_")) {
            return new ThermalProperties(0.15, 0.15, 0.05);
        }

        if (name.startsWith("CHAINMAIL_") || name.startsWith("IRON_") || name.startsWith("GOLDEN_")) {
            return new ThermalProperties(0.1, 0.05, 0.05);
        }

        if (name.startsWith("DIAMOND_")) {
            return new ThermalProperties(0.2, 0.15, 0.15);
        }

        if (name.startsWith("NETHERITE_")) {
            return new ThermalProperties(0.3, 0.2, 0.3);
        }

        if (name.startsWith("TURTLE_")) {
            return new ThermalProperties(0.1, 0.05, 0.05);
        }

        return ThermalProperties.NONE;
    }

}
