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

            // El archivo de disco manda, pero el del JAR queda de respaldo:
            // saveResource(..., false) no sobreescribe, así que las claves que
            // agregue una versión nueva nunca llegarían a un servidor que ya
            // tenía el archivo, y saldrían crudas al jugador.
            YamlConfiguration packaged = loadPackaged(resourcePath);

            if (file.exists()) {
                YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(file);
                if (packaged != null) {
                    onDisk.setDefaults(packaged);
                }
                locales.put(locale, onDisk);
            } else if (packaged != null) {
                locales.put(locale, packaged);
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

    /** El {@code lang/<locale>.yml} tal como viaja dentro del JAR, o null si no está. */
    private YamlConfiguration loadPackaged(String resourcePath) {

        try (java.io.InputStream in = plugin.getResource(resourcePath)) {

            if (in == null) {
                return null;
            }

            return YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));

        } catch (java.io.IOException e) {
            plugin.getLogger().warning("✘ No se pudo leer " + resourcePath + " del JAR: " + e.getMessage());
            return null;
        }
    }

}
