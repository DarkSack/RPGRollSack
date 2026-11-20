package com.sack.rpgroll.emotes;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Valores de {@code config.yml} ya leídos, con los defaults del spec cuando falta una clave.
 *
 * @param castBySchool    emote de conjuro por escuela de magia (id de escuela en minúsculas)
 * @param channelBySchool emote de canalización por escuela
 * @param sneakSwapHotkey  Shift + F (cambiar de mano agachado) abre el menú
 * @param quickActionsHotkey {@code /emotes rapido} abre el diálogo de favoritos
 * @param registerInSrp   copiar el pack de BetterModel a SackResourcePack si está instalado
 */
public record EmotesSettings(int cooldownSeconds, int combatTagSeconds, double moveDistance, double walkingSpeed,
                             int maxFavorites, String limbModel, boolean magicEnabled, String castDefault,
                             Map<String, String> castBySchool, String channelDefault,
                             Map<String, String> channelBySchool, boolean sneakSwapHotkey,
                             boolean quickActionsHotkey, boolean registerInSrp) {

    /** Tope de {@code favorites.max}: es lo que el menú sabe dibujar. */
    public static final int MAX_FAVORITES_CAP = 7;

    public static EmotesSettings from(ConfigurationSection root) {
        return from(root, w -> { });
    }

    /** @param warn recibe el aviso cuando {@code favorites.max} se sale de 1..7 y se limita */
    public static EmotesSettings from(ConfigurationSection root, Consumer<String> warn) {
        int favorites = root.getInt("favorites.max", 7);
        int capped = Math.max(1, Math.min(MAX_FAVORITES_CAP, favorites));
        if (capped != favorites) {
            warn.accept("favorites.max=" + favorites + " fuera de 1.." + MAX_FAVORITES_CAP + ": se usa " + capped);
        }
        return new EmotesSettings(
                root.getInt("cooldown-seconds", 3),
                root.getInt("combat-tag-seconds", 8),
                root.getDouble("cancel.move-distance", 0.3),
                root.getDouble("magic.walking-speed", 0.05),
                capped,
                root.getString("limb-model", "rpgroll_emotes"),
                root.getBoolean("magic.enabled", true),
                root.getString("magic.cast.default", "conjurar"),
                schools(root.getConfigurationSection("magic.cast.schools")),
                root.getString("magic.channel.default", "canalizar"),
                schools(root.getConfigurationSection("magic.channel.schools")),
                root.getBoolean("hotkeys.sneak-swap", true),
                root.getBoolean("hotkeys.quick-actions", true),
                root.getBoolean("resource-pack.register-in-sackresourcepack", true));
    }

    /** El emote de conjuro de la escuela, o el de por defecto si no tiene uno propio. */
    public String castEmoteFor(String schoolId) {
        return pick(castBySchool, schoolId, castDefault);
    }

    /** El emote de canalización de la escuela, o el de por defecto. */
    public String channelEmoteFor(String schoolId) {
        return pick(channelBySchool, schoolId, channelDefault);
    }

    private static String pick(Map<String, String> bySchool, String schoolId, String fallback) {
        return schoolId == null ? fallback : bySchool.getOrDefault(schoolId.toLowerCase(Locale.ROOT), fallback);
    }

    private static Map<String, String> schools(ConfigurationSection section) {
        Map<String, String> out = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String value = section.getString(key);
                if (value != null && !value.isBlank()) {
                    out.put(key.toLowerCase(Locale.ROOT), value);
                }
            }
        }
        return Map.copyOf(out);
    }
}
