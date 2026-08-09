package com.sack.rpgroll.economy.market;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Persiste el estado de oferta/demanda de todos los productos en un solo archivo {@code market/_state.yml}. */
public class MarketStateStore {

    private final File file;

    public MarketStateStore(File dataFolder) {
        File marketFolder = new File(dataFolder, "market");
        marketFolder.mkdirs();
        this.file = new File(marketFolder, "_state.yml");
    }

    public Map<String, MarketState> loadAll() {

        Map<String, MarketState> states = new HashMap<>();

        if (!file.isFile()) {
            return states;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("products");

        if (root == null) {
            return states;
        }

        for (String productId : root.getKeys(false)) {

            ConfigurationSection section = root.getConfigurationSection(productId);
            if (section == null) {
                continue;
            }

            MarketState state = new MarketState(productId);
            state.setSupplyAccumulator(section.getDouble("supply", 0));
            state.setDemandAccumulator(section.getDouble("demand", 0));
            state.setLastRecoveryMillis(section.getLong("last-recovery", System.currentTimeMillis()));
            states.put(productId, state);
        }

        return states;
    }

    public void saveAll(Map<String, MarketState> states) {

        YamlConfiguration config = new YamlConfiguration();

        for (MarketState state : states.values()) {
            String prefix = "products." + state.productId() + ".";
            config.set(prefix + "supply", state.supplyAccumulator());
            config.set(prefix + "demand", state.demandAccumulator());
            config.set(prefix + "last-recovery", state.lastRecoveryMillis());
        }

        try {
            config.save(file);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar el estado de mercado", e);
        }
    }

}
