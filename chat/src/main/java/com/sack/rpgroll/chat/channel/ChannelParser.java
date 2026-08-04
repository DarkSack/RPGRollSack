package com.sack.rpgroll.chat.channel;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

/** Convierte un YAML de channels/ en un {@link ChatChannel}. Solo "id" es obligatorio. */
public class ChannelParser implements ContentParser<ChatChannel> {

    @Override
    public ChatChannel parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        ChannelScope scope = parseEnum(ChannelScope.class, config.getString("scope"), ChannelScope.GLOBAL);
        ChatTextFormat textFormat = parseEnum(ChatTextFormat.class, config.getString("text-format"),
                ChatTextFormat.LEGACY);

        return new ChatChannel(
                id,
                config.getString("display-name", id),
                config.getString("icon", "PAPER"),
                config.getString("color", "WHITE"),
                config.getInt("priority", 0),
                scope,
                config.getDouble("distance", 0),
                config.getString("view-permission", null),
                config.getString("speak-permission", null),
                config.getLong("cooldown-millis", 0),
                config.getString("format", "&7[{channel}] &f{player}&7: &f{message}"),
                textFormat,
                config.getString("join-sound", null),
                config.getBoolean("filter-profanity", true),
                config.getBoolean("filter-caps", true),
                config.getBoolean("allow-urls", false),
                config.getBoolean("default-joined", true),
                config.getBoolean("cross-world", true),
                config.getBoolean("also-action-bar", false));
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String raw, T fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
