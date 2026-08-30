package com.sack.rpgroll.economy.market;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MarketProductDefinitionWriter {

    private final File folder;

    public MarketProductDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "market");
    }

    public void save(MarketProduct product) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", product.id());
        config.set("display-name", product.displayName());
        config.set("icon", product.icon());
        config.set("currency", product.currencyId());
        config.set("base-price", product.basePrice());
        config.set("min-price", product.minPrice());
        config.set("max-price", product.maxPrice());
        config.set("supply-weight", product.supplyWeight());
        config.set("demand-weight", product.demandWeight());
        config.set("volatility", product.volatility());
        config.set("recovery-rate", product.recoveryRate());
        config.set("category", product.category());
        product.seasonTagModifiers().forEach((tag, value) -> config.set("season-modifiers." + tag, value));

        try {
            folder.mkdirs();
            config.save(new File(folder, product.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el producto " + product.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

}
