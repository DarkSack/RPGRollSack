package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registro de tipos de acción para triggers/habilidades de ítem. El motor
 * registra los base (MESSAGE, SOUND, COMMAND, DAMAGE, HEAL, etc.) al
 * arrancar; un addon puede sumar los suyos —
 * {@code api.items().registerAction(...)}.
 */
public class ActionRegistry {

    private final Plugin plugin;
    private final Map<String, ItemActionHandler> handlers = new HashMap<>();

    public ActionRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(String type, ItemActionHandler handler) {
        handlers.put(type.trim().toUpperCase(Locale.ROOT), handler);
    }

    public void execute(ItemAction action, ItemActionContext context) {

        ItemActionHandler handler = handlers.get(action.type());

        if (handler == null) {
            plugin.getLogger().warning("✘ Tipo de acción de ítem desconocido: " + action.type());
            return;
        }

        handler.execute(action, context);
    }

    public void executeAll(Iterable<ItemAction> actions, ItemActionContext context) {
        for (ItemAction action : actions) {
            execute(action, context);
        }
    }

}
