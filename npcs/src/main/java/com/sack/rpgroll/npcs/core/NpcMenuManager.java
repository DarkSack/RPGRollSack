package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.menu.MenuDefinition;
import com.sack.rpgroll.common.menu.MenuParser;

import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

import org.bukkit.plugin.java.JavaPlugin;

public class NpcMenuManager extends ContentManager<MenuDefinition> {

    private final NpcMenuWriter writer;

    public NpcMenuManager(JavaPlugin npcsPlugin) {
        super(owningPlugin(), new YamlLoader(npcsPlugin), "menus", "menú", new MenuParser());
        this.writer = new NpcMenuWriter(npcsPlugin);
    }

    /** Persiste el menú a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(MenuDefinition definition) {
        writer.save(definition);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(NpcMenuManager.class);
    }

}