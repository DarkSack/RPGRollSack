package com.sack.rpgroll.economy.currency;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class CurrencyDefinitionWriter {

    private final File folder;

    public CurrencyDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "currencies");
    }

    public void save(Currency currency) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", currency.id());
        config.set("display-name", currency.displayName());
        config.set("symbol", currency.symbol());
        config.set("decimals", currency.decimals());
        config.set("icon", currency.icon());
        config.set("color", currency.color());
        config.set("min-balance", currency.minBalance());
        config.set("max-balance", currency.maxBalance());
        config.set("permission", currency.permission());
        config.set("exchange-rate-to-base", currency.exchangeRateToBase());
        config.set("is-base", currency.isBase());

        try {
            folder.mkdirs();
            config.save(new File(folder, currency.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la moneda " + currency.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

}
