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

        // Un idioma puede ser de una raza o de una clase: los ids no chocan, así que van en la misma lista.
        java.util.List<String> owners = new java.util.ArrayList<>(config.getStringList("default-for-races"));
        owners.addAll(config.getStringList("default-for-classes"));

        return new Language(
                id,
                config.getString("display-name", id),
                obfuscation.isBlank() ? '?' : obfuscation.charAt(0),
                owners);
    }

}
