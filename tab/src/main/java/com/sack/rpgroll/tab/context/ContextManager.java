package com.sack.rpgroll.tab.context;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.List;

/** Carga los contextos desde plugins/RPGRoll-TAB/contexts/*.yml. */
public class ContextManager extends ContentManager<ContextDefinition> {

    public ContextManager(JavaPlugin tabPlugin) {
        super(resolveCoreInstance(), new YamlLoader(tabPlugin), "contexts", "contexto", new ContextParser());
    }

    /** Contextos ordenados de mayor a menor prioridad — el primero que matchee gana. */
    public List<ContextDefinition> byPriorityDescending() {
        return getAll().stream()
                .sorted(Comparator.comparingInt(ContextDefinition::priority).reversed())
                .toList();
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
