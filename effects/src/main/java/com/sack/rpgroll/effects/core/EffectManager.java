package com.sack.rpgroll.effects.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class EffectManager extends ContentManager<EffectDefinition> {

    private final EffectDefinitionWriter writer;

    public EffectManager(JavaPlugin effectsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(effectsPlugin), "effects", "efecto", new EffectParser());
        this.writer = new EffectDefinitionWriter(effectsPlugin.getDataFolder());
    }

    public void save(EffectDefinition effect) {
        writer.save(effect);
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
