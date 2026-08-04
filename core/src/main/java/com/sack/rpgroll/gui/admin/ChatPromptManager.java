package com.sack.rpgroll.gui.admin;

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
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatPromptManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void prompt(Player player, String question, Consumer<String> callback) {
        player.sendMessage(Component.text(question, NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Escribí en el chat, o 'cancelar' para abortar.", NamedTextColor.GRAY));
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
                event.getPlayer().sendMessage(Component.text("Cancelado.", NamedTextColor.RED));
                return;
            }
            callback.accept(message);
        });
    }

}
