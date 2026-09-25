package com.sack.rpgroll.tab.context;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.List;

/** Carga los contextos desde plugins/RPGRoll-TAB/contexts/*.yml. */
public class ContextManager extends ContentManager<ContextDefinition> {

    public ContextManager(JavaPlugin tabPlugin) {
        super(owningPlugin(), new YamlLoader(tabPlugin), "contexts", "contexto", new ContextParser());
    }

    /** Contextos ordenados de mayor a menor prioridad — el primero que matchee gana. */
    public List<ContextDefinition> byPriorityDescending() {
        return getAll().stream()
                .sorted(Comparator.comparingInt(ContextDefinition::priority).reversed())
                .toList();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(ContextManager.class);
    }

}
