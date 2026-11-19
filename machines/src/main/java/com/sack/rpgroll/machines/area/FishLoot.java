package com.sack.rpgroll.machines.area;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * La pesca vanilla de la Pescadora: qué sale (pez, basura o tesoro, con la suerte del mar como en
 * vanilla) y cuál de cada lista, por pesos. Las listas vienen de {@code fishers.yml}.
 */
public final class FishLoot {

    public enum Category { FISH, JUNK, TREASURE }

    public record Entry(Material material, int weight) {
    }

    private FishLoot() {
    }

    /** Basura 10 % − 2,5 % por nivel de suerte (mínimo 0); tesoro 5 % + 2,1 % por nivel; lo demás, pez. */
    public static Category category(double roll, int luck) {
        double junk = Math.max(0, 0.10 - 0.025 * luck);
        double treasure = 0.05 + 0.021 * luck;
        if (roll < junk) {
            return Category.JUNK;
        }
        return roll < junk + treasure ? Category.TREASURE : Category.FISH;
    }

    /** El de la lista que toca para {@code roll} en [0, 1), según los pesos. Null si la lista está vacía. */
    public static Material pick(List<Entry> entries, double roll) {
        int total = entries.stream().mapToInt(Entry::weight).sum();
        double target = roll * total;
        int cumulative = 0;
        for (Entry entry : entries) {
            cumulative += entry.weight();
            if (target < cumulative) {
                return entry.material();
            }
        }
        return entries.isEmpty() ? null : entries.getLast().material();
    }

    /** {@code "COD:60"} → (COD, 60). Lo que no se entiende se avisa y se salta. */
    public static List<Entry> parse(List<String> raw, Consumer<String> warn) {
        List<Entry> out = new ArrayList<>();
        for (String line : raw) {
            String[] parts = line.split(":");
            Material material = Material.getMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
            Integer weight = null;
            try {
                weight = parts.length == 2 ? Integer.parseInt(parts[1].trim()) : null;
            } catch (NumberFormatException ignored) {
                // se avisa abajo
            }
            if (material == null || weight == null || weight <= 0) {
                warn.accept("fishers.yml (loot): '" + line + "' no es MATERIAL:peso");
                continue;
            }
            out.add(new Entry(material, weight));
        }
        return List.copyOf(out);
    }
}
