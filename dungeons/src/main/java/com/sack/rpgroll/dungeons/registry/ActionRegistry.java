package com.sack.rpgroll.dungeons.registry;

import com.sack.rpgroll.dungeons.core.DungeonAction;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registro de tipos de acción para triggers de dungeon. El motor
 * registra los base (MESSAGE, SOUND, TELEPORT, etc.) al arrancar; un
 * addon puede sumar los suyos.
 */
public class ActionRegistry {

    private final Plugin plugin;
    private final Map<String, DungeonActionHandler> handlers = new HashMap<>();

    public ActionRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(String type, DungeonActionHandler handler) {
        handlers.put(type.trim().toUpperCase(Locale.ROOT), handler);
    }

    public void execute(DungeonAction action, DungeonActionContext context) {

        DungeonActionHandler handler = handlers.get(action.type());

        if (handler == null) {
            plugin.getLogger().warning("✘ Tipo de acción de dungeon desconocido: " + action.type());
            return;
        }

        handler.execute(action, context);
    }

    public void executeAll(Iterable<DungeonAction> actions, DungeonActionContext context) {
        for (DungeonAction action : actions) {
            execute(action, context);
        }
    }

}
