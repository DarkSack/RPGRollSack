package com.sack.rpgroll.sackresourcepack.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Equivalente standalone del {@code LangManager} de {@code :common}, sin
 * depender de él (SackResourcePack no depende de RPGRoll ni de ningún otro
 * plugin — ver {@link com.sack.rpgroll.sackresourcepack.SackResourcePackPlugin}).
 * Carga {@code lang/<codigo>.yml} vía {@link Plugin#saveResource} (nunca
 * sobreescribe un archivo ya existente en disco) y resuelve mensajes con
 * placeholders {@code {nombre}} y fallback de idioma.
 */
public class LangManager {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private final Plugin plugin;
    private final String fallbackLocale;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();
    private YamlConfiguration active;

    public LangManager(Plugin plugin, List<String> supportedLocales, String fallbackLocale) {
        this.plugin = plugin;
        this.fallbackLocale = fallbackLocale;

        for (String locale : supportedLocales) {

            String resourcePath = "lang/" + locale + ".yml";
            File file = new File(plugin.getDataFolder(), resourcePath);

            if (!file.exists()) {
                try {
                    plugin.saveResource(resourcePath, false);
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (file.exists()) {
                locales.put(locale, YamlConfiguration.loadConfiguration(file));
            } else {
                plugin.getLogger().warning("✘ No se encontró lang/" + locale + ".yml — ese idioma quedará sin cargar.");
            }
        }

        reload(fallbackLocale);
    }

    /** Relee el idioma activo — llamar de nuevo tras cambiar "language" en config.yml y recargar. */
    public void reload(String configuredLocale) {
        active = locales.getOrDefault(configuredLocale, locales.get(fallbackLocale));
        if (active == null) {
            plugin.getLogger().warning("✘ Ningún idioma cargó correctamente (ni '" + configuredLocale
                    + "' ni el de respaldo '" + fallbackLocale + "'). Los mensajes se mostrarán como su clave cruda.");
        }
    }

    public String raw(String key, Object... placeholderPairs) {
        String template = active != null ? active.getString(key) : null;

        if (template == null) {
            YamlConfiguration fb = locales.get(fallbackLocale);
            template = fb != null ? fb.getString(key) : null;
        }

        if (template == null) {
            return key;
        }

        for (int i = 0; i + 1 < placeholderPairs.length; i += 2) {
            template = template.replace("{" + placeholderPairs[i] + "}", String.valueOf(placeholderPairs[i + 1]));
        }

        return template;
    }

    public Component component(String key, Object... placeholderPairs) {
        return parse(raw(key, placeholderPairs));
    }

    public void send(CommandSender target, String key, Object... placeholderPairs) {
        target.sendMessage(component(key, placeholderPairs));
    }

    private static Component parse(String text) {
        if (text == null || text.isBlank()) {
            return Component.empty();
        }

        if (text.indexOf('<') >= 0 && text.indexOf('>') >= 0) {
            return MINI_MESSAGE.deserialize(text);
        }

        return LEGACY.deserialize(text);
    }
}
