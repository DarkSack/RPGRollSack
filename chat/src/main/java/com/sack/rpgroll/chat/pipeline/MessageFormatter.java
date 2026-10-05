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
 * {role_suffix}/{context_prefix}/{level_tag}) siempre se resuelven; cualquier %placeholder% que quede
 * en el formato se pasa por PlaceholderAPI si está instalado, lo que da
 * acceso a nivel/clase/raza/job/guild/team/prestigio/reputación/etc. sin
 * que Chat necesite conocer esos addons directamente.
 */
public class MessageFormatter {

    /** {@code .hexColors()} habilita tanto &amp;#RRGGBB como el formato BungeeCord &amp;x&amp;R&amp;R... al deserializar. */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private final ChatRoleManager roleManager;
    private final ChatContextResolver contextResolver;

    public MessageFormatter(ChatRoleManager roleManager, ChatContextResolver contextResolver) {
        this.roleManager = roleManager;
        this.contextResolver = contextResolver;
    }

    public Component format(ChatChannel channel, Player sender, String message) {

        ChatRole role = roleManager.resolveFor(sender).orElse(null);

        // Los %placeholders% se resuelven en el formato ANTES de meter el mensaje (y el nombre del
        // gremio del contexto): con el mensaje dentro, lo que el jugador escribiera entre % % también
        // pasaba por PlaceholderAPI y se mostraba resuelto a todo el canal.
        String text = applyPlaceholderApi(sender, channel.format()
                .replace("{channel}", channel.displayName())
                .replace("{world}", sender.getWorld().getName())
                .replace("{role_prefix}", role != null ? role.prefix() : "")
                .replace("{role_suffix}", role != null ? role.suffix() : ""));

        text = text.replace("{player}", sender.getName())
                .replace("{context_prefix}", contextResolver.contextPrefix(sender))
                .replace("{level_tag}", levelTag(sender))
                .replace("{message}", message);

        return toComponent(text, channel.textFormat());
    }

    /** «[Nv. 12] » con el nivel del Core, o nada si no está instalado: así el formato sirve en los dos casos. */
    private String levelTag(Player sender) {

        return com.sack.rpgroll.common.character.Characters.get()
                .filter(characters -> characters.hasCharacter(sender.getUniqueId()))
                .map(characters -> "&8[&eNv. " + characters.level(sender.getUniqueId()) + "&8] ")
                .orElse("");
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

        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
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
                : LEGACY.deserialize(text);
    }

}
