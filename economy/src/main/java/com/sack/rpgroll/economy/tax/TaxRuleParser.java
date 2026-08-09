package com.sack.rpgroll.economy.tax;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class TaxRuleParser implements ContentParser<TaxRule> {

    @Override
    public TaxRule parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        TaxType type;
        try {
            type = TaxType.valueOf(config.getString("type", "SALE").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            type = TaxType.SALE;
        }

        return new TaxRule(
                id,
                config.getString("display-name", id),
                type,
                config.getDouble("rate-percent", 0),
                config.getStringList("applies-to"),
                config.getBoolean("enabled", true));
    }

}
