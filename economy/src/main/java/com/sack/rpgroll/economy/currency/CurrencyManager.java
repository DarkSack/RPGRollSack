package com.sack.rpgroll.economy.currency;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class CurrencyManager extends ContentManager<Currency> {

    private final CurrencyDefinitionWriter writer;
    private final String defaultCurrencyId;

    public CurrencyManager(JavaPlugin economyPlugin, String defaultCurrencyId) {
        super(resolveCoreInstance(), new YamlLoader(economyPlugin), "currencies", "moneda", new CurrencyParser());
        this.writer = new CurrencyDefinitionWriter(economyPlugin.getDataFolder());
        this.defaultCurrencyId = defaultCurrencyId;
    }

    public void save(Currency currency) {
        writer.save(currency);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    public Currency defaultCurrency() {
        return get(defaultCurrencyId).orElseGet(() -> getAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay ninguna moneda configurada.")));
    }

    public Optional<Currency> base() {
        return getAll().stream().filter(Currency::isBase).findFirst();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
