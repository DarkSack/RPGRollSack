package com.sack.rpgroll.items.registry;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Estadísticas conocidas por el motor. Trae las base listas
 * (health, mana, defense, strength, dexterity, intelligence, luck,
 * critical_chance, critical_damage, attack_speed, armor_penetration,
 * magic_power, magic_resistance, mining_speed, fishing_luck); otro addon
 * puede sumar las suyas con {@link #register(String)} —
 * {@code api.items().registerStat(...)} de la filosofía del addon.
 */
public class StatRegistry {

    private final Set<String> stats = new LinkedHashSet<>();

    public StatRegistry() {
        registerBuiltins();
    }

    private void registerBuiltins() {
        register("health");
        register("mana");
        register("defense");
        register("damage");
        register("speed");
        register("strength");
        register("dexterity");
        register("intelligence");
        register("agility");
        register("luck");
        register("resistance");
        register("armor_penetration");
        register("critical_chance");
        register("critical_damage");
        register("attack_speed");
        register("magic_power");
        register("magic_resistance");
        register("mining_speed");
        register("fishing_luck");
    }

    public void register(String statId) {
        stats.add(statId.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isKnown(String statId) {
        return stats.contains(statId.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> all() {
        return Set.copyOf(stats);
    }

}
