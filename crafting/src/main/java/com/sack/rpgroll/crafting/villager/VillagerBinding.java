package com.sack.rpgroll.crafting.villager;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Guarda en el PDC del aldeano (no en un YAML propio — el PDC de una entidad
 * ya persiste solo con el chunk) qué {@code VillagerTradeDefinition} debe
 * ofrecer. {@code /craftingadmin villager bind} es quien la setea.
 */
public final class VillagerBinding {

    private VillagerBinding() {
    }

    private static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "bound_trades");
    }

    public static void bind(Plugin plugin, AbstractVillager villager, Set<String> tradeIds) {
        villager.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, String.join(",", tradeIds));
    }

    public static void unbind(Plugin plugin, AbstractVillager villager) {
        villager.getPersistentDataContainer().remove(key(plugin));
    }

    public static Set<String> boundTradeIds(Plugin plugin, AbstractVillager villager) {

        String raw = villager.getPersistentDataContainer().get(key(plugin), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }

        return new LinkedHashSet<>(Arrays.asList(raw.split(",")));
    }

    public static boolean isBound(Plugin plugin, AbstractVillager villager) {
        return villager.getPersistentDataContainer().has(key(plugin), PersistentDataType.STRING);
    }

}
