package com.sack.rpgroll.tab.placeholder;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resuelve texto con placeholders {@code {clave}} (motor interno, siempre
 * disponible) y {@code %clave%} (delegado a PlaceholderAPI si está
 * presente — así cualquier expansión ya registrada por Economy/Ascension/
 * Mobs/etc. queda disponible en RPGRoll-TAB sin ninguna dependencia directa
 * a esos módulos).
 * <p>
 * No cachea nada por su cuenta: el llamador (TabList/Scoreboard/Nametag/
 * BelowName/BossBar engines) decide cuándo volver a resolver, disparado por
 * eventos — nunca por un recálculo ciego cada tick.
 */
public class PlaceholderEngine implements TABPlaceholderRegistry {

    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

    private final Plugin plugin;
    private final Map<String, Function<Player, String>> playerPlaceholders = new ConcurrentHashMap<>();
    private final Map<String, Supplier<String>> globalPlaceholders = new ConcurrentHashMap<>();

    public PlaceholderEngine(Plugin plugin) {
        this.plugin = plugin;
        registerBuiltins();
    }

    private void registerBuiltins() {

        register("player", Player::getName);
        register("display_name", p -> PlainTextComponentSerializer.plainText().serialize(p.displayName()));
        register("uuid", p -> p.getUniqueId().toString());
        register("ping", p -> String.valueOf(p.getPing()));
        register("world", p -> p.getWorld().getName());
        register("gamemode", p -> p.getGameMode().name());
        register("health", p -> String.valueOf(Math.round(p.getHealth())));
        register("max_health", p -> {
            AttributeInstance attribute = p.getAttribute(Attribute.MAX_HEALTH);
            return attribute == null ? "20" : String.valueOf(Math.round(attribute.getValue()));
        });
        register("food", p -> String.valueOf(p.getFoodLevel()));
        register("x", p -> String.valueOf(p.getLocation().getBlockX()));
        register("y", p -> String.valueOf(p.getLocation().getBlockY()));
        register("z", p -> String.valueOf(p.getLocation().getBlockZ()));

        registerGlobal("online", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        registerGlobal("max", () -> String.valueOf(Bukkit.getMaxPlayers()));
        registerGlobal("server", () -> Bukkit.getMotd());
        registerGlobal("tps", () -> String.format(Locale.ROOT, "%.1f", Bukkit.getTPS()[0]));
    }

    @Override
    public void register(String key, Function<Player, String> resolver) {
        playerPlaceholders.put(normalize(key), resolver);
    }

    @Override
    public void registerGlobal(String key, Supplier<String> resolver) {
        globalPlaceholders.put(normalize(key), resolver);
    }

    @Override
    public void unregister(String key) {
        String normalized = normalize(key);
        playerPlaceholders.remove(normalized);
        globalPlaceholders.remove(normalized);
    }

    public boolean isRegistered(String key) {
        String normalized = normalize(key);
        return playerPlaceholders.containsKey(normalized) || globalPlaceholders.containsKey(normalized);
    }

    /** Resuelve {@code {placeholders}} internos y, si PlaceholderAPI está presente, {@code %placeholders%}. */
    public String resolve(String template, Player player) {

        if (template == null || template.isEmpty()) {
            return "";
        }

        String result = resolveBraces(template, player);

        if (player != null && result.indexOf('%') >= 0 && isPlaceholderApiPresent()) {
            result = PlaceholderApiBridge.resolve(player, result);
        }

        return result;
    }

    private boolean isPlaceholderApiPresent() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    private String resolveBraces(String template, Player player) {

        if (template.indexOf('{') < 0) {
            return template;
        }

        Matcher matcher = BRACE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String value = resolveKey(matcher.group(1).toLowerCase(Locale.ROOT), player);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveKey(String key, Player player) {

        Function<Player, String> playerResolver = playerPlaceholders.get(key);

        if (playerResolver != null) {

            if (player == null) {
                return "";
            }

            try {
                return playerResolver.apply(player);
            } catch (Exception e) {
                plugin.getLogger().warning("✘ Error resolviendo placeholder {" + key + "}: " + e.getMessage());
                return "";
            }
        }

        Supplier<String> globalResolver = globalPlaceholders.get(key);

        if (globalResolver != null) {
            try {
                return globalResolver.get();
            } catch (Exception e) {
                plugin.getLogger().warning("✘ Error resolviendo placeholder global {" + key + "}: " + e.getMessage());
                return "";
            }
        }

        return null;
    }

    private String normalize(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

}
