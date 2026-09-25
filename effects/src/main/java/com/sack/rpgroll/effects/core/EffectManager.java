package com.sack.rpgroll.effects.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class EffectManager extends ContentManager<EffectDefinition> {

    private final EffectDefinitionWriter writer;

    public EffectManager(JavaPlugin effectsPlugin) {
        super(owningPlugin(), new YamlLoader(effectsPlugin), "effects", "efecto", new EffectParser());
        this.writer = new EffectDefinitionWriter(effectsPlugin.getDataFolder());
    }

    public void save(EffectDefinition effect) {
        writer.save(effect);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(EffectManager.class);
    }

}
