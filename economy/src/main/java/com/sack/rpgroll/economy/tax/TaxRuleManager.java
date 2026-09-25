package com.sack.rpgroll.economy.tax;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TaxRuleManager extends ContentManager<TaxRule> {

    private final TaxRuleDefinitionWriter writer;

    public TaxRuleManager(JavaPlugin economyPlugin) {
        super(owningPlugin(), new YamlLoader(economyPlugin), "tax", "regla tributaria", new TaxRuleParser());
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(TaxRuleManager.class);
    }

}
