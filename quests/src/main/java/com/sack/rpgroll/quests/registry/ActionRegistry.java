package com.sack.rpgroll.quests.registry;

import com.sack.rpgroll.quests.core.QuestAction;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registro de tipos de acción disponibles para eventos de quest/stage y
 * recompensas "extra". El motor registra sus tipos base (MESSAGE, SOUND,
 * COMMAND, GIVE_EXP, GIVE_MONEY, etc.) al arrancar; cualquier addon puede
 * sumar los suyos con {@link #register(String, QuestActionHandler)}.
 */
public class ActionRegistry {

    private final Plugin plugin;
    private final Map<String, QuestActionHandler> handlers = new HashMap<>();

    public ActionRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(String type, QuestActionHandler handler) {
        handlers.put(type.trim().toUpperCase(Locale.ROOT), handler);
    }

    public boolean isRegistered(String type) {
        return handlers.containsKey(type.trim().toUpperCase(Locale.ROOT));
    }

    public void execute(QuestAction action, QuestActionContext context) {

        QuestActionHandler handler = handlers.get(action.type());

        if (handler == null) {
            plugin.getLogger().warning("✘ Tipo de acción de quest desconocido: " + action.type());
            return;
        }

        handler.execute(action, context);
    }

    public void executeAll(Iterable<QuestAction> actions, QuestActionContext context) {
        for (QuestAction action : actions) {
            execute(action, context);
        }
    }

}
