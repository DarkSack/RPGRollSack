package com.sack.rpgroll.guilds.guild;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Requisitos configurables para fundar una guild (spec: "dinero, nivel,
 * quest, ítem, permiso"). El chequeo de quest/ítem vive en el comando
 * (integraciones blandas con Quests/Items) — acá solo se guarda la config.
 */
public record GuildCreationRequirements(double moneyCost, int minLevel, String requiredPermission,
        String requiredQuestId, Material requiredItem, int requiredItemAmount) {

    public static GuildCreationRequirements defaults() {
        return new GuildCreationRequirements(5000, 5, null, null, null, 0);
    }

    public static GuildCreationRequirements fromConfig(ConfigurationSection section) {

        if (section == null) {
            return defaults();
        }

        String itemName = section.getString("required-item", null);
        Material item = null;

        if (itemName != null && !itemName.isBlank()) {
            try {
                item = Material.valueOf(itemName.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new GuildCreationRequirements(
                section.getDouble("money-cost", 5000),
                section.getInt("min-level", 5),
                section.getString("required-permission", null),
                section.getString("required-quest", null),
                item,
                section.getInt("required-item-amount", 1));
    }

}
