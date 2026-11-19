package com.sack.rpgroll.crates.treasure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/** Las tiradas del botín de un tesoro, sin tocar el servidor (los ítems se resuelven aparte). */
public final class TreasureLoot {

    /** Lo que salió en una tirada: el id de la entrada y cuántos. */
    public record Drop(String item, int amount) {
    }

    private TreasureLoot() {
    }

    public static List<Drop> roll(Treasure.Loot loot, RandomGenerator random) {

        double total = loot.entries().stream().mapToDouble(Treasure.Entry::weight).sum();
        int rolls = loot.minRolls() + random.nextInt(loot.maxRolls() - loot.minRolls() + 1);

        List<Drop> drops = new ArrayList<>(rolls);
        for (int i = 0; i < rolls; i++) {
            Treasure.Entry entry = pick(loot.entries(), random.nextDouble() * total);
            drops.add(new Drop(entry.item(), entry.min() + random.nextInt(entry.max() - entry.min() + 1)));
        }
        return drops;
    }

    private static Treasure.Entry pick(List<Treasure.Entry> entries, double target) {
        double accumulated = 0;
        for (Treasure.Entry entry : entries) {
            accumulated += entry.weight();
            if (target < accumulated) {
                return entry;
            }
        }
        return entries.getLast();
    }

    /**
     * Convierte las tiradas en ítems con {@code resolver}; las que no se pueden crear (id desconocido, Items
     * sin instalar) se saltan con un aviso por id.
     */
    public static <T> List<T> resolve(List<Drop> drops, BiFunction<String, Integer, Optional<T>> resolver,
                                      Consumer<String> warn) {

        List<T> items = new ArrayList<>(drops.size());
        Set<String> warned = new HashSet<>();
        for (Drop drop : drops) {
            Optional<T> item = resolver.apply(drop.item(), drop.amount());
            if (item.isPresent()) {
                items.add(item.get());
            } else if (warned.add(drop.item())) {
                warn.accept("ítem de botín desconocido '" + drop.item() + "' (ni material vanilla ni de RPGRoll-Items)");
            }
        }
        return items;
    }

}
