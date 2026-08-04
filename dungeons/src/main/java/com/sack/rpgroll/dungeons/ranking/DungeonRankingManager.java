package com.sack.rpgroll.dungeons.ranking;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda el historial de corridas completadas de cada dungeon en
 * plugins/RPGRoll-Dungeons/rankings/&lt;dungeonId&gt;.yml. No hay
 * "reseteo" explícito de período — diario/semanal/mensual se resuelve
 * filtrando por antigüedad de cada entrada al consultar, así el ranking
 * sigue siendo correcto aunque el server nunca se reinicie en el corte
 * exacto del período.
 */
public class DungeonRankingManager {

    private static final int MAX_ENTRIES_PER_DUNGEON = 200;

    private final Plugin plugin;
    private final File folder;
    private final Map<String, List<DungeonRunResult>> cache = new HashMap<>();

    public DungeonRankingManager(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "rankings");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public void recordRun(DungeonRunResult result) {

        List<DungeonRunResult> entries = getOrLoad(result.dungeonId());
        entries.add(result);

        entries.sort(Comparator.comparingDouble(DungeonRunResult::score).reversed());

        while (entries.size() > MAX_ENTRIES_PER_DUNGEON) {
            entries.remove(entries.size() - 1);
        }

        save(result.dungeonId());
    }

    public List<DungeonRunResult> top(String dungeonId, RankingPeriod period, int limit) {

        long cutoff = period == RankingPeriod.GLOBAL ? 0 : System.currentTimeMillis() - period.windowMillis();

        return getOrLoad(dungeonId).stream()
                .filter(entry -> entry.completedAtMillis() >= cutoff)
                .sorted(Comparator.comparingDouble(DungeonRunResult::score).reversed())
                .limit(limit)
                .toList();
    }

    private List<DungeonRunResult> getOrLoad(String dungeonId) {
        return cache.computeIfAbsent(dungeonId.toLowerCase(), this::load);
    }

    private List<DungeonRunResult> load(String dungeonId) {

        List<DungeonRunResult> results = new ArrayList<>();
        File file = new File(folder, dungeonId + ".yml");

        if (!file.exists()) {
            return results;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> raw = config.getList("runs");

        if (raw == null) {
            return results;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }

            try {
                String difficultyId = String.valueOf(map.get("difficulty"));
                @SuppressWarnings("unchecked")
                List<String> players = map.get("players") instanceof List<?> playerList
                        ? playerList.stream().map(String::valueOf).toList()
                        : List.of();
                long completedAt = Long.parseLong(String.valueOf(map.get("completed-at")));
                long duration = Long.parseLong(String.valueOf(map.get("duration-millis")));
                int deaths = map.get("deaths") != null ? Integer.parseInt(String.valueOf(map.get("deaths"))) : 0;
                double damage = map.get("damage") != null ? Double.parseDouble(String.valueOf(map.get("damage"))) : 0.0;

                results.add(new DungeonRunResult(dungeonId, difficultyId, players, completedAt, duration, deaths,
                        damage));
            } catch (NumberFormatException | NullPointerException ignored) {
            }
        }

        return results;
    }

    private void save(String dungeonId) {

        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> raw = new ArrayList<>();

        for (DungeonRunResult result : getOrLoad(dungeonId)) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("difficulty", result.difficultyId());
            entry.put("players", result.playerNames());
            entry.put("completed-at", result.completedAtMillis());
            entry.put("duration-millis", result.durationMillis());
            entry.put("deaths", result.deaths());
            entry.put("damage", result.totalDamageDealt());

            raw.add(entry);
        }

        config.set("runs", raw);

        try {
            config.save(new File(folder, dungeonId.toLowerCase() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando ranking de " + dungeonId + ": " + e.getMessage());
        }
    }

}
