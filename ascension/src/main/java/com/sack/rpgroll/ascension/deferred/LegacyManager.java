package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.Optional;

public class LegacyManager extends ContentManager<LegacyTier> {

    private final LegacyDefinitionWriter writer;

    public LegacyManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "legacy", "legado", new LegacyParser());
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(LegacyManager.class);
    }

}
