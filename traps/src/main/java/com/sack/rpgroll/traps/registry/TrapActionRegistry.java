package com.sack.rpgroll.traps.registry;

import com.sack.rpgroll.traps.core.TrapAction;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registro de tipos de acción para trampas. El motor registra los base
 * (MESSAGE, EXPLOSION, TOGGLE_BLOCKS, etc.) al arrancar; otro addon podría
 * sumar los suyos en el futuro del mismo modo que RPGRoll-Mobs expone
 * {@code api.mobs().registerAction(...)}.
 */
public class TrapActionRegistry {

    private final Plugin plugin;
    private final Map<String, TrapActionHandler> handlers = new HashMap<>();

    public TrapActionRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(String type, TrapActionHandler handler) {
        handlers.put(type.trim().toUpperCase(Locale.ROOT), handler);
    }

    public void execute(TrapAction action, TrapActionContext context) {

        TrapActionHandler handler = handlers.get(action.type());

        if (handler == null) {
            plugin.getLogger().warning("✘ Tipo de acción de trampa desconocido: " + action.type());
            return;
        }

        try {
            handler.execute(action, context);
        } catch (Exception e) {
            plugin.getLogger().warning("✘ Error ejecutando acción " + action.type() + " de '"
                    + context.sourceId() + "': " + e.getMessage());
        }
    }

    public void executeAll(Iterable<TrapAction> actions, TrapActionContext context) {
        for (TrapAction action : actions) {
            execute(action, context);
        }
    }

}
