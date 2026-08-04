package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.Optional;

public class LegacyManager extends ContentManager<LegacyTier> {

    private final LegacyDefinitionWriter writer;

    public LegacyManager(JavaPlugin ascensionPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ascensionPlugin), "legacy", "legado", new LegacyParser());
        this.writer = new LegacyDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(LegacyTier tier) {
        writer.save(tier);
        reload();
    }

    /** El tier de legado más alto que el jugador ya califica para reclamar con su prestigio actual. */
    public Optional<LegacyTier> highestAvailable(int prestigeCount) {
        return getAll().stream()
                .filter(tier -> tier.requiredPrestige() <= prestigeCount)
                .max(Comparator.comparingInt(LegacyTier::requiredPrestige));
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
