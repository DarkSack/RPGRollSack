package com.sack.rpgroll.economy.market;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class MarketRegionManager extends ContentManager<MarketRegion> {

    private final MarketRegionDefinitionWriter writer;

    public MarketRegionManager(JavaPlugin economyPlugin) {
        super(owningPlugin(), new YamlLoader(economyPlugin), "market-regions", "región de mercado",
                new MarketRegionParser());
        this.writer = new MarketRegionDefinitionWriter(economyPlugin.getDataFolder());
    }

    public void save(MarketRegion region) {
        writer.save(region);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    /** Primera región que contiene el punto — sin superposición, gana la que aparece primero. */
    public Optional<MarketRegion> resolve(String world, double x, double y, double z) {
        return getAll().stream().filter(region -> region.contains(world, x, y, z)).findFirst();
    }

    @Override
    protected boolean optionalContent() {
        return true;
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(MarketRegionManager.class);
    }

}
