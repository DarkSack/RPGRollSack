package com.sack.rpgroll.economy.market;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MarketProductManager extends ContentManager<MarketProduct> {

    private final MarketProductDefinitionWriter writer;

    public MarketProductManager(JavaPlugin economyPlugin) {
        super(resolveCoreInstance(), new YamlLoader(economyPlugin), "market", "producto de mercado",
                new MarketProductParser());
        this.writer = new MarketProductDefinitionWriter(economyPlugin.getDataFolder());
    }

    public void save(MarketProduct product) {
        writer.save(product);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
