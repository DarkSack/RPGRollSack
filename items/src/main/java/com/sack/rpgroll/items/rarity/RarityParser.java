package com.sack.rpgroll.items.rarity;

import com.sack.rpgroll.common.content.ContentParser;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class RarityParser implements ContentParser<Rarity> {

    @Override
    public Rarity parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        TextColor color = parseColor(config.getString("color", "WHITE"));
        boolean glow = config.getBoolean("glow", false);
        Sound sound = parseSound(config.getString("sound"));
        Particle particle = parseParticle(config.getString("particle"));

        return new Rarity(id, displayName, color, glow, sound, particle);
    }

    private TextColor parseColor(String raw) {

        NamedTextColor named = NamedTextColor.NAMES.value(raw.trim().toLowerCase(Locale.ROOT));
        if (named != null) {
            return named;
        }

        TextColor hex = TextColor.fromHexString(raw.trim());
        return hex != null ? hex : NamedTextColor.WHITE;
    }

    private Sound parseSound(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Particle parseParticle(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Particle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
