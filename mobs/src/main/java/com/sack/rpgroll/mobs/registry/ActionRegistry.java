package com.sack.rpgroll.mobs.registry;

import com.sack.rpgroll.mobs.core.MobAction;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registro de tipos de acción para triggers/skills de mob. El motor
 * registra los base (MESSAGE, SOUND, DAMAGE, SUMMON, etc.) al arrancar;
 * un addon puede sumar los suyos — {@code api.mobs().registerAction(...)}.
 */
public class ActionRegistry {

    private final Plugin plugin;
    private final Map<String, MobActionHandler> handlers = new HashMap<>();

    public ActionRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(String type, MobActionHandler handler) {
        handlers.put(type.trim().toUpperCase(Locale.ROOT), handler);
    }

    public void execute(MobAction action, MobActionContext context) {

        MobActionHandler handler = handlers.get(action.type());

        if (handler == null) {
            plugin.getLogger().warning("✘ Tipo de acción de mob desconocido: " + action.type());
            return;
        }

        handler.execute(action, context);
    }

    public void executeAll(Iterable<MobAction> actions, MobActionContext context) {
        for (MobAction action : actions) {
            execute(action, context);
        }
    }

}
