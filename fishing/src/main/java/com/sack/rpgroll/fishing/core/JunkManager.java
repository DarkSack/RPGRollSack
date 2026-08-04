package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class JunkManager extends ContentManager<Junk> {

    private final JunkDefinitionWriter writer;

    public JunkManager(JavaPlugin fishingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(fishingPlugin), "junk", "basura", new JunkParser());
        this.writer = new JunkDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(Junk junk) {
        writer.save(junk);
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
