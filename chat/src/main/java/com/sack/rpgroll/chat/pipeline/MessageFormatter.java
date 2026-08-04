package com.sack.rpgroll.chat.pipeline;

import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChatTextFormat;
import com.sack.rpgroll.chat.context.ChatContextResolver;
import com.sack.rpgroll.chat.role.ChatRole;
import com.sack.rpgroll.chat.role.ChatRoleManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;

/**
 * Construye el Component final de un mensaje — spec "Formatos Dinámicos".
 * Los tokens nativos ({player}/{message}/{channel}/{world}/{role_prefix}/
 * {role_suffix}) siempre se resuelven; cualquier %placeholder% que quede
 * en el formato se pasa por PlaceholderAPI si está instalado, lo que da
 * acceso a nivel/clase/raza/job/guild/team/prestigio/reputación/etc. sin
 * que Chat necesite conocer esos addons directamente.
 */
public class MessageFormatter {

    private final ChatRoleManager roleManager;
    private final ChatContextResolver contextResolver;

    public MessageFormatter(ChatRoleManager roleManager, ChatContextResolver contextResolver) {
        this.roleManager = roleManager;
        this.contextResolver = contextResolver;
    }

    public Component format(ChatChannel channel, Player sender, String message) {

        ChatRole role = roleManager.resolveFor(sender).orElse(null);

        String text = channel.format()
                .replace("{player}", sender.getName())
                .replace("{message}", message)
                .replace("{channel}", channel.displayName())
                .replace("{world}", sender.getWorld().getName())
                .replace("{role_prefix}", role != null ? role.prefix() : "")
                .replace("{role_suffix}", role != null ? role.suffix() : "")
                .replace("{context_prefix}", contextResolver.contextPrefix(sender));

        text = applyPlaceholderApi(sender, text);

        return toComponent(text, channel.textFormat());
    }

    /** Para mensajes sin emisor real (ej. anuncios de Sistema/Eventos): sin {player}/roles. */
    public Component formatBroadcast(ChatChannel channel, String message) {

        String text = channel.format()
                .replace("{player}", "")
                .replace("{message}", message)
                .replace("{channel}", channel.displayName())
                .replace("{role_prefix}", "")
                .replace("{role_suffix}", "");

        return toComponent(text, channel.textFormat());
    }

    private String applyPlaceholderApi(Player player, String text) {

        if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text;
        }

        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable ignored) {
            return text;
        }
    }

    private Component toComponent(String text, ChatTextFormat format) {
        return format == ChatTextFormat.MINIMESSAGE
                ? MiniMessage.miniMessage().deserialize(text)
                : LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

}
