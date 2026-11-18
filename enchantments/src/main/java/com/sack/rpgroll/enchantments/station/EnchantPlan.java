package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.enchantments.core.Rarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lo que el jugador va armando en el súper encantador: el nivel destino de cada encantamiento, qué
 * choca con qué (sobre el resultado final) y cuánto cuesta. No toca el ítem: eso lo hace la estación
 * al pagar.
 */
final class EnchantPlan {

    enum Kind { VANILLA, CUSTOM }

    /**
     * Un encantamiento posible. {@code id}: {@code minecraft:<clave>} o {@code rpgroll:<id>}.
     * {@code vanillaMax}: el máximo vanilla (los del plugin, su propio máximo). Las entradas no
     * editables son las que el ítem ya tiene y aquí no se tocan (tesoros, maldiciones): solo cuentan
     * para los conflictos.
     */
    record Entry(String id, Kind kind, int current, int cap, int vanillaMax, Rarity rarity,
                 Set<String> conflicts, boolean editable) {
    }

    record Cost(int xp, int lapis) {
        boolean free() {
            return xp == 0 && lapis == 0;
        }
    }

    /** Precios: vanilla hasta su máximo, vanilla por encima (como siempre) y del plugin por rareza. */
    record Prices(int vanillaLevels, int vanillaLapis, int base, int step, int lapisPerLevel,
                  Map<Rarity, Integer> rarityLevels, Map<Rarity, Integer> rarityLapis) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Map<String, Integer> targets = new LinkedHashMap<>();
    private final Prices prices;

    EnchantPlan(List<Entry> entries, Prices prices) {
        this.prices = prices;
        entries.forEach(e -> this.entries.put(e.id(), e));
        reset();
    }

    /** Las que se pueden tocar: primero las que ya tiene, luego las del plugin por rareza y luego las vanilla. */
    List<Entry> editable() {
        return entries.values().stream().filter(Entry::editable)
                .sorted(Comparator.comparing((Entry e) -> e.current() > 0 ? 0 : e.kind() == Kind.CUSTOM ? 1 : 2)
                        .thenComparing(e -> e.kind() == Kind.CUSTOM ? -e.rarity().ordinal() : 0)
                        .thenComparing(Entry::id))
                .toList();
    }

    Entry entry(String id) {
        return entries.get(id);
    }

    int target(String id) {
        return targets.getOrDefault(id, 0);
    }

    /** Fija el nivel destino (recortado entre 0 y el tope). No deja poner uno bloqueado por un conflicto. */
    boolean set(String id, int level) {
        Entry e = entries.get(id);
        if (e == null || !e.editable()) {
            return false;
        }
        int clamped = Math.max(0, Math.min(e.cap(), level));
        if (clamped > 0 && blocked(id)) {
            return false;
        }
        targets.put(id, clamped);
        return true;
    }

    /** Lo que impide ponerlo: los que quedarán puestos y chocan con él. */
    List<String> blockers(String id) {
        Entry e = entries.get(id);
        List<String> out = new ArrayList<>();
        if (e == null || target(id) > 0) {
            return out;
        }
        for (Entry other : entries.values()) {
            if (!other.id().equals(id) && target(other.id()) > 0
                    && (e.conflicts().contains(other.id()) || other.conflicts().contains(id))) {
                out.add(other.id());
            }
        }
        return out;
    }

    boolean blocked(String id) {
        return !blockers(id).isEmpty();
    }

    /** Suma de cada nivel que se sube; bajar o quitar no cuesta ni devuelve nada. */
    Cost cost() {
        int xp = 0;
        int lapis = 0;
        for (Entry e : entries.values()) {
            for (int level = e.current() + 1; level <= target(e.id()); level++) {
                Cost step = step(e, level);
                xp += step.xp();
                lapis += step.lapis();
            }
        }
        return new Cost(xp, lapis);
    }

    private Cost step(Entry e, int level) {
        if (e.kind() == Kind.CUSTOM) {
            return new Cost(prices.rarityLevels().getOrDefault(e.rarity(), 0) * level,
                    prices.rarityLapis().getOrDefault(e.rarity(), 0));
        }
        if (level <= e.vanillaMax()) {
            return new Cost(prices.vanillaLevels() * level, prices.vanillaLapis());
        }
        return new Cost(SuperEnchanter.xpFor(level, e.vanillaMax(), prices.base(), prices.step()),
                prices.lapisPerLevel() * Math.max(1, level - e.vanillaMax()));
    }

    /** Solo lo que cambia: id → nivel destino (0 = quitarlo). */
    Map<String, Integer> changes() {
        Map<String, Integer> out = new LinkedHashMap<>();
        entries.values().forEach(e -> {
            if (target(e.id()) != e.current()) {
                out.put(e.id(), target(e.id()));
            }
        });
        return out;
    }

    void reset() {
        targets.clear();
        entries.values().forEach(e -> targets.put(e.id(), e.current()));
    }
}
