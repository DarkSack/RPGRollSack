package com.sack.rpgroll.mobs.registry;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Estadísticas de mob conocidas por el motor. Otro addon puede sumar las
 * suyas con {@link #register(String)} — {@code api.mobs().registerStat(...)}.
 */
public class MobStatRegistry {

    private final Set<String> stats = new LinkedHashSet<>();

    public MobStatRegistry() {
        registerBuiltins();
    }

    private void registerBuiltins() {
        register("health");
        register("mana");
        register("shield");
        register("damage");
        register("defense");
        register("speed");
        register("armor");
        register("magic_armor");
        register("magic_damage");
        register("critical_chance");
        register("critical_damage");
        register("dodge");
        register("accuracy");
        register("resistance");
        register("knockback_resistance");
        register("attack_speed");
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
