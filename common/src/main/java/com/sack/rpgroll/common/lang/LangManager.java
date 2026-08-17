package com.sack.rpgroll.common.lang;

import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga los archivos {@code lang/<codigo>.yml} empaquetados en el JAR de un
 * plugin (uno por idioma soportado), los copia a disco la primera vez vía
 * {@link ResourceCopier} (nunca sobreescribe una traducción ya editada por
 * el admin) y resuelve mensajes por clave con reemplazo de placeholders
 * {@code {nombre}} y fallback al idioma de respaldo si falta la clave o el
 * idioma configurado no cargó.
 * <p>
 * Cada plugin instancia la suya propia — no hay estado compartido entre
 * plugins; cada uno lee su propio {@code config.yml} -> {@code language}.
 */
public class LangManager {

    private final Plugin plugin;
    private final String fallbackLocale;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();
    private YamlConfiguration active;

    public LangManager(Plugin plugin, List<String> supportedLocales, String fallbackLocale) {
        this.plugin = plugin;
        this.fallbackLocale = fallbackLocale;

        new ResourceCopier(plugin).copyDirectories(List.of("lang"));

        for (String locale : supportedLocales) {
            File file = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
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

    /**
     * Texto resuelto (legacy {@code &}/MiniMessage sin parsear todavía) para
     * {@code key}, con pares {@code placeholder, valor} reemplazando
     * {@code {placeholder}} en el template. Si la clave no existe en el
     * idioma activo cae al de respaldo; si tampoco existe ahí, devuelve la
     * clave misma (nunca null, nunca revienta).
     */
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
        return ComponentUtils.parse(raw(key, placeholderPairs));
    }

    public void send(CommandSender target, String key, Object... placeholderPairs) {
        target.sendMessage(component(key, placeholderPairs));
    }
}
