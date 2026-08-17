package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

/** Captura el próximo mensaje de chat de un jugador y lo entrega a un callback — usado por las GUIs de Economy Studio. */
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

    public void prompt(Player player, String question, Consumer<String> callback) {
        player.sendMessage(Component.text(question, NamedTextColor.YELLOW));
        player.sendMessage(lang.component("prompt.footer", "keyword", lang.raw("prompt.cancel_keyword")));
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
