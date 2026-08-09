package com.sack.rpgroll.economy.tax;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TaxRuleDefinitionWriter {

    private final File folder;

    public TaxRuleDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "tax");
    }

    public void save(TaxRule rule) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", rule.id());
        config.set("display-name", rule.displayName());
        config.set("type", rule.type().name());
        config.set("rate-percent", rule.ratePercent());
        config.set("applies-to", List.copyOf(rule.appliesTo()));
        config.set("enabled", rule.enabled());

        try {
            folder.mkdirs();
            config.save(new File(folder, rule.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la regla tributaria " + rule.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

}
