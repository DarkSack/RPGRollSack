package com.sack.rpgroll.chat.language;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageParser implements ContentParser<Language> {

    @Override
    public Language parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String obfuscation = config.getString("obfuscation-char", "?");

        return new Language(
                id,
                config.getString("display-name", id),
                obfuscation.isBlank() ? '?' : obfuscation.charAt(0),
                config.getStringList("default-for-races"));
    }

}
