package com.sack.rpgroll.npcs.listener;

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

/**
 * Captura el próximo mensaje de chat de un jugador y lo entrega a un
 * callback, en vez de dejar que se transmita como chat normal. Genérico —
 * usado por la GUI de admin para pedir nombre, skin, acciones, etc.
 */
public class ChatPromptManager implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatPromptManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void prompt(Player player, String question, Consumer<String> callback) {
        player.sendMessage(net.kyori.adventure.text.Component.text(question,
                net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        player.sendMessage(net.kyori.adventure.text.Component.text(
                "Escribe en el chat, o 'cancelar' para abortar.",
                net.kyori.adventure.text.format.NamedTextColor.GRAY));
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
                event.getPlayer().sendMessage(net.kyori.adventure.text.Component.text(
                        "Cancelado.", net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
            callback.accept(message);
        });
    }

}