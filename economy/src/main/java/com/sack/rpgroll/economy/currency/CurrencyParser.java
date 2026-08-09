package com.sack.rpgroll.economy.currency;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class CurrencyParser implements ContentParser<Currency> {

    @Override
    public Currency parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Currency(
                id,
                config.getString("display-name", id),
                config.getString("symbol", "$"),
                config.getInt("decimals", 2),
                config.getString("icon", "SUNFLOWER"),
                config.getString("color", "GOLD"),
                config.getDouble("min-balance", 0),
                config.getDouble("max-balance", 0),
                config.getString("permission"),
                config.getDouble("exchange-rate-to-base", 1.0),
                config.getBoolean("is-base", false));
    }

}
