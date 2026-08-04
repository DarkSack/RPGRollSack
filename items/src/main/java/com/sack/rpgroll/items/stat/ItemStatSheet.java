package com.sack.rpgroll.items.stat;

import java.util.HashMap;
import java.util.Map;

/** Totales de stats aportados por todos los ítems RPGRoll equipados por un jugador. */
public class ItemStatSheet {

    private final Map<String, Double> totals = new HashMap<>();

    public double get(String stat) {
        return totals.getOrDefault(stat, 0.0);
    }

    public void add(String stat, double amount) {
        totals.merge(stat, amount, Double::sum);
    }

    public void clear() {
        totals.clear();
    }

    public Map<String, Double> all() {
        return Map.copyOf(totals);
    }

}
