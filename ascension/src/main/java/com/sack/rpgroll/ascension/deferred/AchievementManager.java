package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class AchievementManager extends ContentManager<Achievement> {

    private final AchievementDefinitionWriter writer;

    public AchievementManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "achievements", "logro",
                new AchievementParser());
        this.writer = new AchievementDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(Achievement achievement) {
        writer.save(achievement);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(AchievementManager.class);
    }

}
