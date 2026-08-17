package com.sack.rpgroll.chat.listener;

import com.sack.rpgroll.chat.pipeline.ChatMessagePipeline;
import com.sack.rpgroll.common.lang.LangManager;

import io.papermc.paper.event.player.AsyncChatEvent;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Punto de entrada del chat vanilla — delega toda la lógica a {@link ChatMessagePipeline}. */
public class ChatListener implements Listener {

    private final ChatMessagePipeline pipeline;
    private final Plugin plugin;
    private final LangManager lang;

    public ChatListener(ChatMessagePipeline pipeline, Plugin plugin, LangManager lang) {
        this.pipeline = pipeline;
        this.plugin = plugin;
        this.lang = lang;
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
                case NO_ACTIVE_CHANNEL -> lang.send(player, "channel.no_active");
                case NO_PERMISSION -> lang.send(player, "channel.no_permission_speak");
                case ON_COOLDOWN -> lang.send(player, "antispam.on_cooldown");
                case FLOOD -> lang.send(player, "antispam.flood");
                case REPETITION -> lang.send(player, "antispam.repetition");
                case BANNED_WORD -> lang.send(player, "antispam.banned_word");
                case URL_BLOCKED -> lang.send(player, "antispam.url_blocked");
                case EXCESSIVE_CAPS -> lang.send(player, "antispam.excessive_caps");
            }
        });
    }

}
