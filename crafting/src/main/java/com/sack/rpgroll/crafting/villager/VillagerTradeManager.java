package com.sack.rpgroll.crafting.villager;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class VillagerTradeManager extends ContentManager<VillagerTradeDefinition> {

    private final VillagerTradeDefinitionWriter writer;

    public VillagerTradeManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "villager-trades", "comercio de aldeano",
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

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
