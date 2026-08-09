package com.sack.rpgroll.economy.market;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class MarketProductParser implements ContentParser<MarketProduct> {

    @Override
    public MarketProduct parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new MarketProduct(
                id,
                config.getString("display-name", id),
                config.getString("icon", "PAPER"),
                config.getString("currency"),
                config.getDouble("base-price", 1.0),
                config.getDouble("min-price", 0),
                config.getDouble("max-price", 0),
                config.getDouble("supply-weight", 1.0),
                config.getDouble("demand-weight", 1.0),
                config.getDouble("volatility", 0.1),
                config.getDouble("recovery-rate", 0.02),
                config.getString("category", "misc"));
    }

}
