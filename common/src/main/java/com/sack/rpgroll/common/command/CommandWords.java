package com.sack.rpgroll.common.command;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Los nombres de cada comando y de sus subcomandos en español, inglés y portugués, para que el
 * jugador escriba en su idioma. Cada módulo trae los suyos en {@code command-words.yml}:
 * <pre>
 * commands:
 *   guild:                         # el nombre con el que se registra
 *     names: {es: gremio, pt: guilda}
 *     subcommands:
 *       create: {es: crear, pt: criar}
 * </pre>
 * {@link BrigadierCommands} registra los nombres como alias, traduce el primer argumento a su
 * forma de siempre antes de llamar al comando (así ningún comando cambia) y sugiere los
 * subcomandos en el idioma del cliente.
 */
public final class CommandWords {

    private static final CommandWords EMPTY = new CommandWords(Map.of());

    private record Entry(Map<String, String> names, Map<String, Map<String, String>> subcommands) {
    }

    private final Map<String, Entry> commands;

    private CommandWords(Map<String, Entry> commands) {
        this.commands = commands;
    }

    public static CommandWords empty() {
        return EMPTY;
    }

    /** {@code command-words.yml} del jar del plugin; vacío si no lo trae. */
    public static CommandWords load(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("command-words.yml")) {
            if (in == null) {
                return EMPTY;
            }
            return parse(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (java.io.IOException e) {
            return EMPTY;
        }
    }

    static CommandWords parse(ConfigurationSection root) {
        ConfigurationSection section = root.getConfigurationSection("commands");
        if (section == null) {
            return EMPTY;
        }
        Map<String, Entry> out = new LinkedHashMap<>();
        for (String command : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(command);
            if (c == null) {
                continue;
            }
            Map<String, Map<String, String>> subs = new LinkedHashMap<>();
            ConfigurationSection s = c.getConfigurationSection("subcommands");
            if (s != null) {
                for (String sub : s.getKeys(false)) {
                    subs.put(sub.toLowerCase(Locale.ROOT), languages(s.getConfigurationSection(sub)));
                }
            }
            out.put(command.toLowerCase(Locale.ROOT), new Entry(languages(c.getConfigurationSection("names")), subs));
        }
        return new CommandWords(out);
    }

    private static Map<String, String> languages(ConfigurationSection section) {
        Map<String, String> out = new LinkedHashMap<>();
        if (section != null) {
            for (String lang : section.getKeys(false)) {
                String word = section.getString(lang);
                if (word != null && !word.isBlank()) {
                    out.put(lang.toLowerCase(Locale.ROOT), word.toLowerCase(Locale.ROOT).trim());
                }
            }
        }
        return out;
    }

    /** Los nombres del comando en otros idiomas (para registrarlos como alias). */
    public List<String> names(String command) {
        Entry e = commands.get(command.toLowerCase(Locale.ROOT));
        if (e == null) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>(e.names().values());
        out.remove(command.toLowerCase(Locale.ROOT));
        return List.copyOf(out);
    }

    /** El subcomando de siempre que corresponde a {@code word}, escrita en cualquier idioma. */
    public String canonical(String command, String word) {
        Entry e = commands.get(command.toLowerCase(Locale.ROOT));
        if (e == null || word == null) {
            return word;
        }
        String lower = word.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Map<String, String>> sub : e.subcommands().entrySet()) {
            if (sub.getKey().equals(lower) || sub.getValue().containsValue(lower)) {
                return sub.getKey();
            }
        }
        return word;
    }

    /** Cómo se escribe el subcomando {@code canonical} en {@code lang}; él mismo si no hay traducción. */
    public String localized(String command, String canonical, String lang) {
        Entry e = commands.get(command.toLowerCase(Locale.ROOT));
        if (e == null || canonical == null) {
            return canonical;
        }
        Map<String, String> sub = e.subcommands().get(canonical.toLowerCase(Locale.ROOT));
        return sub == null ? canonical : sub.getOrDefault(lang, canonical);
    }

    /**
     * Sugerencias del primer argumento: las de siempre ({@code all}) pasadas al idioma del jugador y
     * filtradas por lo escrito. Si lo escrito solo encaja con la forma de siempre (alguien que
     * escribe en inglés), se le enseña esa.
     */
    public List<String> suggestFirst(String command, List<String> all, String typed, String lang) {
        String prefix = typed == null ? "" : typed.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String canonical : all) {
            String shown = localized(command, canonical, lang);
            if (shown.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(shown);
            } else if (canonical.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(canonical);
            }
        }
        return out;
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    /** Si la tabla traduce algún subcomando de {@code command}. */
    public boolean hasSubcommands(String command) {
        Entry e = commands.get(command.toLowerCase(Locale.ROOT));
        return e != null && !e.subcommands().isEmpty();
    }

    /** es, pt o en (lo demás cae en inglés). */
    public static String language(Locale locale) {
        String lang = locale == null ? "es" : locale.getLanguage();
        return switch (lang) {
            case "es", "pt" -> lang;
            default -> "en";
        };
    }

    /** El idioma de quien escribe: el de su cliente, o español en la consola. */
    public static String language(CommandSender sender) {
        return sender instanceof Player player ? language(player.locale()) : "es";
    }
}
