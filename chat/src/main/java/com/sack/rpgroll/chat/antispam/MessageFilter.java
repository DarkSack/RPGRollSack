package com.sack.rpgroll.chat.antispam;

import com.sack.rpgroll.chat.channel.ChatChannel;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Censura y reemplazos de texto por canal — spec "Filtros: Censura, Reemplazos, Emojis". */
public class MessageFilter {

    private final AntiSpamConfig antiSpamConfig;
    private final Map<String, String> replacements;

    public MessageFilter(AntiSpamConfig antiSpamConfig, Map<String, String> replacements) {
        this.antiSpamConfig = antiSpamConfig;
        this.replacements = replacements;
    }

    public String apply(ChatChannel channel, String message) {

        String result = message;

        for (var entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        if (channel.filterProfanity()) {
            for (String banned : antiSpamConfig.bannedWords()) {
                Pattern pattern = Pattern.compile(Pattern.quote(banned), Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(result);
                result = matcher.replaceAll("*".repeat(Math.max(3, banned.length())));
            }
        }

        if (channel.filterCaps()) {
            result = softenCaps(result);
        }

        return result;
    }

    /** Si el mensaje es mayormente mayúsculas, lo pasa a minúscula-con-inicial — no lo bloquea, solo lo suaviza. */
    private String softenCaps(String message) {

        String letters = message.replaceAll("[^a-zA-Z]", "");

        if (letters.length() < 8) {
            return message;
        }

        long upper = letters.chars().filter(Character::isUpperCase).count();

        if ((upper * 100 / letters.length()) < 80) {
            return message;
        }

        String lower = message.toLowerCase(java.util.Locale.ROOT);
        return lower.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

}
