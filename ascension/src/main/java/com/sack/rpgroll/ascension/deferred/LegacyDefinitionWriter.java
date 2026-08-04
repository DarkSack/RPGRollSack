package com.sack.rpgroll.ascension.deferred;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LegacyDefinitionWriter {

    private final File folder;

    public LegacyDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "legacy");
    }

    public void save(LegacyTier tier) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", tier.id());
        config.set("required-prestige", tier.requiredPrestige());
        config.set("permanent-exp-bonus-percent", tier.permanentExpBonusPercent());
        config.set("bonus-stat-points", tier.bonusStatPoints());

        try {
            folder.mkdirs();
            config.save(new File(folder, tier.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el legado " + tier.id(), e);
        }
    }

}
