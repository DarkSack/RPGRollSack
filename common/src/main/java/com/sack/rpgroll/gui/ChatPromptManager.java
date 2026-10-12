package com.sack.rpgroll.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.format.NamedTextColor;
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

/**
 * Captura el próximo mensaje de chat de un jugador y lo entrega a un callback — lo usan las GUIs
 * para pedir un nombre, un precio o una cantidad. Cada plugin crea y registra el suyo, con su
 * propio {@link LangManager}: los textos del pie y de "cancelado" salen de su lang.
 * <p>
 * Antes cada módulo llevaba su copia (21); un arreglo en una no llegaba a las demás.
 */
public class ChatPromptManager implements Listener {

    private final Plugin plugin;
    private final LangManager lang;
    private final String footerKey;
    private final String cancelKeywordKey;
    private final String cancelledKey;
    // Se escribe en el hilo principal y se lee en el del chat (asíncrono).
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatPromptManager(Plugin plugin, LangManager lang) {
        this(plugin, lang, "prompt.footer", "prompt.cancel_keyword", "prompt.cancelled");
    }

    /** Para los módulos cuyo lang llama de otra forma a estos tres textos. */
    public ChatPromptManager(Plugin plugin, LangManager lang, String footerKey, String cancelKeywordKey,
            String cancelledKey) {
        this.plugin = plugin;
        this.lang = lang;
        this.footerKey = footerKey;
        this.cancelKeywordKey = cancelKeywordKey;
        this.cancelledKey = cancelledKey;
    }

    public LangManager lang() {
        return lang;
    }

    /** @param question la pregunta ya escrita (texto, no una clave del lang). */
    public void prompt(Player player, String question, Consumer<String> callback) {
        player.sendMessage(ComponentUtils.parseWithDefault(question, NamedTextColor.YELLOW));
        ask(player, callback);
    }

    /** @param questionKey clave del lang con la pregunta a mostrarle al jugador. */
    public void promptKey(Player player, String questionKey, Consumer<String> callback, Object... placeholderPairs) {
        lang.send(player, questionKey, placeholderPairs);
        ask(player, callback);
    }

    private void ask(Player player, Consumer<String> callback) {
        lang.send(player, footerKey, "keyword", lang.raw(cancelKeywordKey));
        pending.put(player.getUniqueId(), callback);
    }

    // LOWEST: se cancela antes de que el chat de guild o de equipo (LOW), RPGRoll-Chat
    // u otro plugin difunda la respuesta, que puede ser un precio o una cantidad.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();
        Consumer<String> callback = pending.remove(uuid);

        if (callback == null) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase(lang.raw(cancelKeywordKey))) {
                lang.send(event.getPlayer(), cancelledKey);
                return;
            }
            callback.accept(message);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

}
