package com.sack.rpgroll.sackeffects.gui;

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

/** Captura el próximo mensaje de chat de un jugador y lo entrega a un callback — usado por las GUIs de admin de contenido. */
public class ChatPromptManager implements Listener {

    private final Plugin plugin;
    private final LangManager langManager;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatPromptManager(Plugin plugin, LangManager langManager) {
        this.plugin = plugin;
        this.langManager = langManager;
    }

    /** {@code question} ya viene resuelto (traducido) por quien llama, vía {@code langManager.raw(...)}. */
    public void prompt(Player player, String question, Consumer<String> callback) {
        player.sendMessage(Component.text(question, NamedTextColor.YELLOW));
        langManager.send(player, "prompt.cancel_hint");
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
            if (message.equalsIgnoreCase("cancelar")) {
                langManager.send(event.getPlayer(), "prompt.cancelled");
                return;
            }
            callback.accept(message);
        });
    }

}
