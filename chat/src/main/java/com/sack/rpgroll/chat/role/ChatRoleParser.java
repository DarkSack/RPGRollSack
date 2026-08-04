package com.sack.rpgroll.chat.role;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class ChatRoleParser implements ContentParser<ChatRole> {

    @Override
    public ChatRole parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new ChatRole(
                id,
                config.getString("prefix", ""),
                config.getString("suffix", ""),
                config.getString("color", "WHITE"),
                config.getString("icon", ""),
                config.getInt("priority", 0),
                config.getString("permission", null));
    }

}
