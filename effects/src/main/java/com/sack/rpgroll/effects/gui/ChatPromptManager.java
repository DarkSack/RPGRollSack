package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.common.lang.LangManager;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Captura el próximo mensaje de chat de un jugador y lo entrega a un callback — usado por las GUIs de admin de contenido. */
public class ChatPromptManager implements Listener {

    private final Plugin plugin;
    private final LangManager lang;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatPromptManager(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public LangManager lang() {
        return lang;
    }

    /** @param questionKey clave de lang.yml con la pregunta a mostrarle al jugador. */
    public void prompt(Player player, String questionKey, Consumer<String> callback) {
        lang.send(player, questionKey);
        lang.send(player, "prompt.footer", "keyword", lang.raw("prompt.cancel_keyword"));
        pending.put(player.getUniqueId(), callback);
    }

    /** Igual que {@link #prompt(Player, String, Consumer)} pero con placeholders {@code {nombre}} para la pregunta. */
    public void prompt(Player player, String questionKey, Consumer<String> callback, Object... placeholderPairs) {
        lang.send(player, questionKey, placeholderPairs);
        lang.send(player, "prompt.footer", "keyword", lang.raw("prompt.cancel_keyword"));
        pending.put(player.getUniqueId(), callback);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();
        Consumer<String> callback = pending.remove(uuid);

        if (callback == null) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase(lang.raw("prompt.cancel_keyword"))) {
                lang.send(event.getPlayer(), "prompt.cancelled");
                return;
            }
            callback.accept(message);
        });
    }

}
