package com.sack.rpgroll.worldforge.core;

import com.sack.rpgroll.worldforge.api.Problem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Texts of {@code messages.yml} (MiniMessage). Keys missing from the server's copy fall back
 * to the jar's, so an old file never shows raw keys. Values are always inserted as plain
 * text ({@link Placeholder#unparsed}): a world or profile name cannot inject formatting.
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final YamlConfiguration defaults;
    private YamlConfiguration current;

    public Messages(Supplier<Reader> jarCopy) {
        this.defaults = YamlConfiguration.loadConfiguration(jarCopy.get());
        this.current = defaults;
    }

    public static Messages fromJar(Class<?> owner) {
        return new Messages(() -> new InputStreamReader(owner.getResourceAsStream("/messages.yml"), StandardCharsets.UTF_8));
    }

    /** Reads the server's copy; returns the problems found (the jar copy stays as fallback). */
    public List<String> load(Path file) {
        List<String> problems = new ArrayList<>();
        if (!Files.exists(file)) {
            return problems;
        }
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.load(file.toFile());
            y.setDefaults(defaults);
            current = y;
        } catch (Exception e) {
            problems.add("messages.yml no se pudo leer (" + e.getMessage() + "); se usan los textos de serie.");
            current = defaults;
        }
        return problems;
    }

    /** @param pairs name, value, name, value… */
    public Component get(String key, String... pairs) {
        String raw = current.getString(key, defaults.getString(key, key));
        TagResolver.Builder b = TagResolver.builder();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            b.resolver(Placeholder.unparsed(pairs[i], pairs[i + 1] == null ? "" : pairs[i + 1]));
        }
        return MM.deserialize(raw, b.build());
    }

    public void send(CommandSender to, String key, String... pairs) {
        to.sendMessage(get("prefix").append(get(key, pairs)));
    }

    public void sendPlain(CommandSender to, Component text) {
        to.sendMessage(get("prefix").append(text));
    }

    public void problems(CommandSender to, List<Problem> problems) {
        for (Problem p : problems) {
            to.sendMessage(Component.text("  " + p, p.isError() ? NamedTextColor.RED : NamedTextColor.GOLD));
        }
    }
}
