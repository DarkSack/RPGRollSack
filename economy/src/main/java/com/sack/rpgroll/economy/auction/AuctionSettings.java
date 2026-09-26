package com.sack.rpgroll.economy.auction;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * La sección {@code auction-house} de config.yml.
 *
 * @param limits         permiso-sufijo → publicaciones máximas; gana el mayor
 *                       que tenga el jugador ({@code rpgrolleconomy.auction.limit.<sufijo>})
 * @param blockedDataKeys claves de PersistentDataContainer ({@code plugin:clave})
 *                       que impiden publicar un ítem (mochilas ligadas, brújulas…)
 */
public record AuctionSettings(
        List<String> aliases,
        long durationMillis,
        int maxListings,
        Map<String, Integer> limits,
        double minPrice,
        double maxPrice,
        double listingFeePercent,
        double minBidIncrementPercent,
        long antiSnipeMillis,
        String currency,
        Set<Material> blacklist,
        List<String> blockedDataKeys) {

    public static final String LIMIT_PERMISSION_PREFIX = "rpgrolleconomy.auction.limit.";

    public static AuctionSettings from(ConfigurationSection config, Consumer<String> warn) {

        ConfigurationSection section = config.getConfigurationSection("auction-house");
        if (section == null) {
            section = config.createSection("auction-house");
        }

        // auction-default-duration-hours es la clave de antes; se respeta si no hay una nueva.
        double hours = section.getDouble("duration-hours", config.getDouble("auction-default-duration-hours", 48));
        if (hours <= 0) {
            warn.accept("auction-house.duration-hours debe ser mayor que 0; se usa 48.");
            hours = 48;
        }

        Map<String, Integer> limits = new LinkedHashMap<>();
        ConfigurationSection limitSection = section.getConfigurationSection("limits");
        if (limitSection != null) {
            for (String key : limitSection.getKeys(false)) {
                limits.put(key.toLowerCase(Locale.ROOT), Math.max(0, limitSection.getInt(key)));
            }
        }

        Set<Material> blacklist = EnumSet.noneOf(Material.class);
        for (String name : section.getStringList("blacklist")) {
            try {
                blacklist.add(Material.valueOf(name.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                warn.accept("auction-house.blacklist: material desconocido «" + name + "».");
            }
        }

        double minPrice = Math.max(0.01, section.getDouble("min-price", 1));
        double maxPrice = section.getDouble("max-price", 0);
        if (maxPrice > 0 && maxPrice < minPrice) {
            warn.accept("auction-house.max-price es menor que min-price; se quita el máximo.");
            maxPrice = 0;
        }

        return new AuctionSettings(
                section.getStringList("aliases"),
                (long) (hours * 3_600_000L),
                Math.max(0, section.getInt("max-listings", 5)),
                Map.copyOf(limits),
                minPrice,
                maxPrice,
                clampPercent(section.getDouble("listing-fee-percent", 0), "listing-fee-percent", warn),
                clampPercent(section.getDouble("min-bid-increment-percent", 5), "min-bid-increment-percent", warn),
                Math.max(0, section.getLong("anti-snipe-seconds", 30)) * 1000L,
                section.getString("currency", ""),
                blacklist,
                section.getStringList("blocked-data-keys"));
    }

    private static double clampPercent(double value, String key, Consumer<String> warn) {
        if (value < 0 || value > 100) {
            warn.accept("auction-house." + key + " debe estar entre 0 y 100; se usa " + Math.max(0, Math.min(100, value)) + ".");
        }
        return Math.max(0, Math.min(100, value));
    }

    /** Publicaciones a la vez para quien tenga estos permisos de límite (0 = sin límite). */
    public int limitFor(java.util.function.Predicate<String> hasPermission) {

        int best = maxListings;

        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            if (hasPermission.test(LIMIT_PERMISSION_PREFIX + entry.getKey())) {
                if (entry.getValue() == 0) {
                    return 0;
                }
                best = best == 0 ? 0 : Math.max(best, entry.getValue());
            }
        }

        return best;
    }

    /** null si el precio es válido; si no, la clave del mensaje de error. */
    public String checkPrice(double price) {

        if (!Double.isFinite(price) || price <= 0) {
            return "invalid_price";
        }
        if (price < minPrice) {
            return "price_too_low";
        }
        if (maxPrice > 0 && price > maxPrice) {
            return "price_too_high";
        }
        return null;
    }

    public double fee(double price) {
        return Math.round(price * listingFeePercent) / 100.0;
    }

    /** La puja mínima que supera a la actual. Sin pujas, basta el precio inicial. */
    public double minNextBid(AuctionListing listing) {

        if (!listing.hasBidder()) {
            return listing.startPrice();
        }

        double step = Math.max(1, listing.currentBid() * minBidIncrementPercent / 100.0);
        return Math.ceil((listing.currentBid() + step) * 100) / 100.0;
    }

}
