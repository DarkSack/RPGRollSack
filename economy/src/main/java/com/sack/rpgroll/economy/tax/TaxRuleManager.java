package com.sack.rpgroll.economy.tax;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TaxRuleManager extends ContentManager<TaxRule> {

    private final TaxRuleDefinitionWriter writer;

    public TaxRuleManager(JavaPlugin economyPlugin) {
        super(resolveCoreInstance(), new YamlLoader(economyPlugin), "tax", "regla tributaria", new TaxRuleParser());
        this.writer = new TaxRuleDefinitionWriter(economyPlugin.getDataFolder());
    }

    public void save(TaxRule rule) {
        writer.save(rule);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    public List<TaxRule> rulesFor(TaxType type) {
        return getAll().stream().filter(r -> r.enabled() && r.type() == type).toList();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
