package com.sack.rpgroll.chat.listener;

import com.sack.rpgroll.chat.pipeline.ChatMessagePipeline;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/** Punto de entrada del chat vanilla — delega toda la lógica a {@link ChatMessagePipeline}. */
public class ChatListener implements Listener {

    private final ChatMessagePipeline pipeline;
    private final Plugin plugin;

    public ChatListener(ChatMessagePipeline pipeline, Plugin plugin) {
        this.pipeline = pipeline;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {

        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        var player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {

            var result = pipeline.send(player, message);

            switch (result) {
                case OK -> {
                }
                case NO_ACTIVE_CHANNEL -> player.sendMessage(Component.text(
                        "No tenés un canal activo — usá /channel join <canal>.", NamedTextColor.RED));
                case NO_PERMISSION -> player.sendMessage(Component.text(
                        "No tenés permiso para hablar en este canal.", NamedTextColor.RED));
                case ON_COOLDOWN -> player.sendMessage(Component.text(
                        "Esperá un momento antes de volver a hablar en este canal.", NamedTextColor.RED));
                case FLOOD -> player.sendMessage(Component.text(
                        "Estás enviando mensajes demasiado rápido.", NamedTextColor.RED));
                case REPETITION -> player.sendMessage(Component.text(
                        "No repitas el mismo mensaje.", NamedTextColor.RED));
                case BANNED_WORD -> player.sendMessage(Component.text(
                        "Tu mensaje contiene una palabra prohibida.", NamedTextColor.RED));
                case URL_BLOCKED -> player.sendMessage(Component.text(
                        "No se permiten enlaces en este canal.", NamedTextColor.RED));
                case EXCESSIVE_CAPS -> player.sendMessage(Component.text(
                        "Bajá un poco las mayúsculas.", NamedTextColor.RED));
            }
        });
    }

}
