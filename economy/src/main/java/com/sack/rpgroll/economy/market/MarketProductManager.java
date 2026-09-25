package com.sack.rpgroll.economy.market;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class MarketProductManager extends ContentManager<MarketProduct> {

    private final MarketProductDefinitionWriter writer;

    public MarketProductManager(JavaPlugin economyPlugin) {
        super(owningPlugin(), new YamlLoader(economyPlugin), "market", "producto de mercado",
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(MarketProductManager.class);
    }

}
