package com.sack.rpgroll.extras.menu;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.menu.MenuDefinition;
import com.sack.rpgroll.common.menu.MenuParser;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Los menús de {@code menus/}: mismo formato que los de RPGRoll-NPCs (el motor
 * vive en RPGRoll-Lib), pero sin depender de ese módulo.
 */
public class ExtrasMenuManager extends ContentManager<MenuDefinition> {

    public ExtrasMenuManager(JavaPlugin extrasPlugin) {
        super(owningPlugin(), new YamlLoader(extrasPlugin), "menus", "menú", new MenuParser());
    }

    @Override
    protected boolean optionalContent() {
        return true;
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(ExtrasMenuManager.class);
    }

}
