package com.sack.rpgroll.recipes.gui;

import com.sack.rpgroll.common.lang.LangManager;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** El texto de búsqueda se escribe en el chat (mismo patrón que los ChatPromptManager de RPGRoll). */
public final class SearchPrompt implements Listener {

    private final Plugin plugin;
    private final LangManager lang;
    // Se escribe en el hilo principal y se lee en el del chat (asíncrono).
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public SearchPrompt(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void ask(Player player, Consumer<String> callback) {
        pending.put(player.getUniqueId(), callback);
        player.closeInventory();
        lang.send(player, "search.ask");
        lang.send(player, "search.footer", "keyword", lang.raw("search.cancel_keyword"));
    }

    // LOWEST: se cancela antes de que cualquier plugin de chat difunda lo que se buscó.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {

        Consumer<String> callback = pending.remove(event.getPlayer().getUniqueId());
        if (callback == null) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!event.getPlayer().isOnline()) {
                return;
            }
            callback.accept(message.equalsIgnoreCase(lang.raw("search.cancel_keyword")) ? null : message);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
