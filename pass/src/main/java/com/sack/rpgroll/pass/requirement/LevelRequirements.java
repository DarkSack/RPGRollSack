package com.sack.rpgroll.pass.requirement;

import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo que un nivel del pase exige además de haber llegado a él. Vale para las
 * dos pistas. Todo es opcional: 0, null o una lista vacía no exige nada.
 * <pre>
 * requirements:
 *   character-level: 15        nivel de personaje (RPGRoll)
 *   playtime: 5h               tiempo jugado en la temporada sin estar AFK (30m, 5h, 1d, 2h30m)
 *   season-day: 7              a partir del día 7 de la temporada
 *   available-from: 2026-10-15 a partir de esa fecha
 *   votes: 10                  votos totales
 *   daily-streak: 5            racha de recompensa diaria
 *   missions: 20               misiones del pase completadas esta temporada
 *   quests: [dragon_intro]     misiones de RPGRoll-Quests completadas
 *   permission: rank.vip       un permiso...
 *   permission-name: "&6VIP"   ...y cómo se le muestra al jugador
 * </pre>
 */
public record LevelRequirements(int characterLevel, int playtimeMinutes, int seasonDay, LocalDate availableFrom,
        int votes, int dailyStreak, int missions, List<String> quests, String permission, String permissionName) {

    public static final LevelRequirements NONE = new LevelRequirements(0, 0, 0, null, 0, 0, 0, List.of(), null, null);

    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*([dhm])");

    private static final List<String> KNOWN = List.of("character-level", "playtime", "season-day", "available-from",
            "votes", "daily-streak", "missions", "quests", "permission", "permission-name");

    public LevelRequirements {
        quests = quests == null ? List.of() : List.copyOf(quests);
        permission = permission == null || permission.isBlank() ? null : permission.trim();
        permissionName = permissionName == null || permissionName.isBlank() ? permission : permissionName;
    }

    public boolean isEmpty() {
        return characterLevel <= 0 && playtimeMinutes <= 0 && seasonDay <= 1 && availableFrom == null && votes <= 0
                && dailyStreak <= 0 && missions <= 0 && quests.isEmpty() && permission == null;
    }

    /** Lee el bloque {@code requirements}; lo que no entiende lo avisa y lo ignora. */
    public static LevelRequirements parse(ConfigurationSection section, String where, Consumer<String> warn) {

        if (section == null) {
            return NONE;
        }

        int playtime = 0;
        Object rawPlaytime = section.get("playtime");
        if (rawPlaytime != null) {
            try {
                playtime = minutes(rawPlaytime.toString());
            } catch (IllegalArgumentException e) {
                warn.accept(where + ": playtime '" + rawPlaytime + "' no se entiende (usa 30m, 5h, 1d o 2h30m).");
            }
        }

        LocalDate from = null;
        Object rawDate = section.get("available-from");
        if (rawDate != null) {
            try {
                // YAML lee 2026-10-15 sin comillas como fecha (a medianoche UTC), no como texto.
                from = rawDate instanceof Date date ? date.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
                        : LocalDate.parse(rawDate.toString().trim());
            } catch (DateTimeParseException e) {
                warn.accept(where + ": available-from tiene que ser una fecha AAAA-MM-DD.");
            }
        }

        for (String key : section.getKeys(false)) {
            if (!KNOWN.contains(key)) {
                warn.accept(where + ": requisito desconocido '" + key + "' (se ignora).");
            }
        }

        return new LevelRequirements(
                Math.max(0, section.getInt("character-level")),
                playtime,
                Math.max(0, section.getInt("season-day")),
                from,
                Math.max(0, section.getInt("votes")),
                Math.max(0, section.getInt("daily-streak")),
                Math.max(0, section.getInt("missions")),
                section.getStringList("quests"),
                section.getString("permission"),
                section.getString("permission-name"));
    }

    /** {@code 90}, {@code 90m}, {@code 5h}, {@code 1d}, {@code 2h30m} → minutos. */
    public static int minutes(String raw) {

        String text = raw.trim().toLowerCase(Locale.ROOT);

        if (text.matches("\\d+")) {
            return Integer.parseInt(text);
        }

        Matcher matcher = DURATION.matcher(text);
        int total = 0;
        int consumed = 0;

        while (matcher.find()) {
            if (!text.substring(consumed, matcher.start()).isBlank()) {
                throw new IllegalArgumentException(raw);
            }
            int value = Integer.parseInt(matcher.group(1));
            total += switch (matcher.group(2)) {
                case "d" -> value * 24 * 60;
                case "h" -> value * 60;
                default -> value;
            };
            consumed = matcher.end();
        }

        if (consumed == 0 || !text.substring(consumed).isBlank()) {
            throw new IllegalArgumentException(raw);
        }

        return total;
    }

    /** Minutos → {@code 2h 30m}, para enseñárselo al jugador. */
    public static String format(int minutes) {

        int days = minutes / (24 * 60);
        int hours = minutes % (24 * 60) / 60;
        int mins = minutes % 60;
        StringBuilder text = new StringBuilder();

        if (days > 0) {
            text.append(days).append("d ");
        }
        if (hours > 0) {
            text.append(hours).append("h ");
        }
        if (mins > 0 || text.isEmpty()) {
            text.append(mins).append('m');
        }

        return text.toString().trim();
    }

}
