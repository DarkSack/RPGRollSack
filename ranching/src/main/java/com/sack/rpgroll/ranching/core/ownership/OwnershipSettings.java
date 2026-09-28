package com.sack.rpgroll.ranching.core.ownership;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Las secciones {@code ownership}, {@code recall} y {@code market} del config.yml.
 *
 * @param serverShop    animales que vende el servidor (fundadores nuevos)
 * @param sellToServer  lo que paga el servidor por especie, antes de multiplicar por la calidad del animal
 */
public record OwnershipSettings(boolean protect, boolean claimUnowned, int maxAnimalsPerPlayer, int recallCooldownSeconds,
        boolean recallCrossWorld, boolean recreateMissing, boolean marketEnabled, double minPrice, double maxPrice,
        double taxPercent, List<ServerOffer> serverShop, Map<String, Double> sellToServer) {

    /** Un animal que vende el servidor. {@code sex} null = al azar. */
    public record ServerOffer(String speciesId, String breedId, String sex, double price) {
    }

    public static OwnershipSettings from(ConfigurationSection config) {

        ConfigurationSection ownership = section(config, "ownership");
        ConfigurationSection recall = section(config, "recall");
        ConfigurationSection market = section(config, "market");

        List<ServerOffer> shop = new ArrayList<>();

        for (Map<?, ?> raw : market == null ? List.<Map<?, ?>>of() : market.getMapList("server-shop")) {

            Object species = raw.get("species");
            Object price = raw.get("price");

            if (species == null || !(price instanceof Number number) || number.doubleValue() <= 0) {
                continue;
            }

            Object breed = raw.get("breed");
            Object sex = raw.get("sex");
            shop.add(new ServerOffer(species.toString().toLowerCase(Locale.ROOT),
                    breed == null ? null : breed.toString().toLowerCase(Locale.ROOT),
                    sex == null || sex.toString().equalsIgnoreCase("RANDOM") ? null : sex.toString().toUpperCase(Locale.ROOT),
                    number.doubleValue()));
        }

        Map<String, Double> sellToServer = new HashMap<>();
        ConfigurationSection sell = market == null ? null : market.getConfigurationSection("sell-to-server");

        if (sell != null) {
            for (String species : sell.getKeys(false)) {
                if (sell.getDouble(species) > 0) {
                    sellToServer.put(species.toLowerCase(Locale.ROOT), sell.getDouble(species));
                }
            }
        }

        return new OwnershipSettings(
                bool(ownership, "protect", true),
                bool(ownership, "claim-unowned", true),
                ownership == null ? 0 : Math.max(0, ownership.getInt("max-animals-per-player", 0)),
                recall == null ? 5 : Math.max(0, recall.getInt("cooldown-seconds", 5)),
                bool(recall, "cross-world", true),
                bool(recall, "recreate-missing", true),
                bool(market, "enabled", true),
                market == null ? 1 : Math.max(0.01, market.getDouble("min-price", 1)),
                market == null ? 1_000_000 : market.getDouble("max-price", 1_000_000),
                market == null ? 0 : Math.max(0, Math.min(100, market.getDouble("tax-percent", 0))),
                List.copyOf(shop), Map.copyOf(sellToServer));
    }

    private static ConfigurationSection section(ConfigurationSection config, String path) {
        return config == null ? null : config.getConfigurationSection(path);
    }

    private static boolean bool(ConfigurationSection section, String path, boolean fallback) {
        return section == null ? fallback : section.getBoolean(path, fallback);
    }

}
