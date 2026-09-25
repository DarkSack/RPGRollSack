package com.sack.rpgroll.crafting.villager;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class VillagerTradeManager extends ContentManager<VillagerTradeDefinition> {

    private final VillagerTradeDefinitionWriter writer;

    public VillagerTradeManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "villager-trades", "comercio de aldeano",
                new VillagerTradeParser());
        this.writer = new VillagerTradeDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(VillagerTradeDefinition trade) {
        writer.save(trade);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(VillagerTradeManager.class);
    }

}
