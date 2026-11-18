package com.sack.rpgroll.economy.servershop;

import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tope diario de venta por jugador y producto, para que una cantera o una granja automática no
 * fabriquen dinero sin fin. Las primeras {@code full-price-units} del día se pagan enteras, las
 * siguientes hasta {@code max-units} a {@code reduced-ratio} y, pasado eso, la tienda no compra
 * más de ese producto hasta el día siguiente. Lo usan /tienda y el comprador.
 * <p>
 * shortcut: el recuento vive en memoria y un reinicio lo vacía; con el reinicio diario del servidor
 * da igual, pero si se reinicia varias veces al día habría que guardarlo en disco.
 */
public final class SellQuota {

    /** Cuántas unidades se compran de las pedidas y por cuánto en total. */
    public record Deal(int units, double money) {
    }

    private int fullUnits = 0;
    private int maxUnits = 0;
    private double reducedRatio = 0.3;
    private LocalDate day = LocalDate.now();
    private final Map<UUID, Map<String, Integer>> sold = new HashMap<>();

    /** Sección {@code server-shop.daily-limits}; sin ella (o con 0) no hay tope. */
    public void configure(ConfigurationSection section) {
        if (section == null) {
            fullUnits = 0;
            maxUnits = 0;
            return;
        }
        fullUnits = Math.max(0, section.getInt("full-price-units", 0));
        maxUnits = Math.max(fullUnits, section.getInt("max-units", 0));
        reducedRatio = Math.max(0, Math.min(1, section.getDouble("reduced-ratio", reducedRatio)));
    }

    public boolean enabled() {
        return fullUnits > 0;
    }

    private Map<String, Integer> of(UUID player) {
        LocalDate today = LocalDate.now();
        if (!today.equals(day)) {
            day = today;
            sold.clear();
        }
        return sold.computeIfAbsent(player, id -> new HashMap<>());
    }

    public int soldToday(UUID player, String key) {
        return of(player).getOrDefault(key, 0);
    }

    /** Lo que se pagaría por {@code units} más de {@code key} hoy, a {@code unitPrice} cada una. No apunta nada. */
    public Deal quote(UUID player, String key, int units, double unitPrice) {
        return quote(soldToday(player, key), units, unitPrice);
    }

    /** Como {@link #quote(UUID, String, int, double)} contando además {@code pending} ya ofrecidas en la misma venta. */
    public Deal quote(UUID player, String key, int pending, int units, double unitPrice) {
        return quote(soldToday(player, key) + pending, units, unitPrice);
    }

    Deal quote(int already, int units, double unitPrice) {
        if (!enabled()) {
            return new Deal(units, units * unitPrice);
        }
        int left = Math.max(0, maxUnits - already);
        int take = Math.min(units, left);
        int full = Math.min(take, Math.max(0, fullUnits - already));
        double money = unitPrice * (full + (take - full) * reducedRatio);
        return new Deal(take, Math.round(money * 100) / 100.0);
    }

    public void record(UUID player, String key, int units) {
        if (enabled() && units > 0) {
            of(player).merge(key, units, Integer::sum);
        }
    }
}
