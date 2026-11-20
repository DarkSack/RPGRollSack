package com.sack.rpgroll.emotes.catalog;

import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/** Convierte el YAML de un emote en un {@link Emote}, o lanza {@link IllegalArgumentException} en español. */
public final class EmoteParser {

    private EmoteParser() {
    }

    public static Emote parse(String fileId, ConfigurationSection yml, String defaultModel) {
        return parse(fileId, yml, defaultModel, w -> { });
    }

    /** Igual, y avisa por {@code warn} de lo que se ignora sin romper el emote. */
    public static Emote parse(String fileId, ConfigurationSection yml, String defaultModel, Consumer<String> warn) {
        String animation = yml.getString("animation");
        if (animation == null || animation.isBlank()) {
            throw new IllegalArgumentException("falta la clave «animation»");
        }
        String id = yml.getString("id", fileId).trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("el id está vacío");
        }

        String unlock = yml.getString("unlock", "free").trim().toLowerCase(Locale.ROOT);
        if (!unlock.equals("free") && !unlock.equals("locked")) {
            throw new IllegalArgumentException("«unlock» debe ser free o locked, no «" + unlock + "»");
        }

        return new Emote(id,
                yml.getString("name", id),
                yml.getString("icon", "PLAYER_HEAD"),
                yml.getString("category", "social").trim().toLowerCase(Locale.ROOT),
                animation,
                yml.getString("model", defaultModel),
                yml.getBoolean("loop", false),
                unlock.equals("locked"),
                yml.getString("rarity", "common").trim().toLowerCase(Locale.ROOT),
                sound(yml.getConfigurationSection("sound")),
                particles(yml.getConfigurationSection("particles")),
                cancel(yml),
                yml.getBoolean("hidden", false),
                yml.getString("how-to-unlock"),
                music(yml.getConfigurationSection("music"), id, fileId, warn));
    }

    private static EmoteSound sound(ConfigurationSection s) {
        if (s == null || s.getString("id") == null) {
            return null;
        }
        return new EmoteSound(s.getString("id"), (float) s.getDouble("volume", 1.0), (float) s.getDouble("pitch", 1.0));
    }

    private static EmoteMusic music(ConfigurationSection s, String emoteId, String fileId, Consumer<String> warn) {
        if (s == null) {
            return null;
        }
        String sound = s.getString("sound");
        if (sound == null || sound.isBlank()) {
            warn.accept("Emote «" + emoteId + "»: «music» sin «sound», se ignora la música.");
            return null;
        }
        sound = sound.trim().toLowerCase(Locale.ROOT);
        if (!Key.parseable(sound)) {
            warn.accept("Emote «" + emoteId + "» (" + fileId + ".yml): «music.sound» no es una clave válida («"
                    + sound + "»), se ignora la música.");
            return null;
        }
        float volume = (float) Math.max(0.0, Math.min(4.0, s.getDouble("volume", 1.0)));
        return new EmoteMusic(sound, volume, Math.max(0.0, s.getDouble("length", 0.0)));
    }

    private static EmoteParticles particles(ConfigurationSection s) {
        if (s == null || s.getString("type") == null) {
            return null;
        }
        return new EmoteParticles(s.getString("type"), s.getInt("count", 1));
    }

    private static Set<CancelReason> cancel(ConfigurationSection yml) {
        if (!yml.contains("cancel")) {
            return EnumSet.allOf(CancelReason.class);
        }
        // «cancel: move» (texto suelto) cuenta como lista de un elemento.
        List<String> raw = yml.isString("cancel") ? List.of(yml.getString("cancel")) : yml.getStringList("cancel");
        Set<CancelReason> out = EnumSet.noneOf(CancelReason.class);
        for (String s : raw) {
            try {
                out.add(CancelReason.valueOf(s.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("motivo de corte desconocido en «cancel»: «" + s + "»");
            }
        }
        return out;
    }
}
