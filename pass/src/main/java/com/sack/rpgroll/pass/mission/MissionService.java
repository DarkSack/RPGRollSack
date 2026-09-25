package com.sack.rpgroll.pass.mission;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.pass.PassClock;
import com.sack.rpgroll.pass.player.PassPlayer;
import com.sack.rpgroll.pass.season.PassService;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sortea las misiones diarias y semanales, lleva su avance y da los puntos de
 * pase al completarlas. El sorteo es el mismo para todo el servidor ese día
 * (la semilla es la fecha), así todos pueden hablar de "las misiones de hoy".
 */
public class MissionService {

    private final PassService pass;
    private final LangManager lang;
    private final PassClock clock;

    private Map<String, Mission> missions = Map.of();
    private int dailyCount = 3;
    private int weeklyCount = 3;

    public MissionService(PassService pass, LangManager lang, PassClock clock) {
        this.pass = pass;
        this.lang = lang;
        this.clock = clock;
    }

    public void load(List<Mission> loaded, int dailyCount, int weeklyCount) {

        Map<String, Mission> byId = new LinkedHashMap<>();
        loaded.forEach(mission -> byId.put(mission.id(), mission));

        this.missions = Collections.unmodifiableMap(byId);
        this.dailyCount = Math.max(0, dailyCount);
        this.weeklyCount = Math.max(0, weeklyCount);
    }

    public int count() {
        return missions.size();
    }

    /** Las misiones vigentes de un tipo de renovación, ya sorteadas para este jugador. */
    public List<Mission> active(Player player, MissionScope scope) {

        PassPlayer state = refreshed(player);

        List<String> ids = switch (scope) {
            case DAILY -> state.dailyMissions();
            case WEEKLY -> state.weeklyMissions();
            case SEASON -> pool(MissionScope.SEASON).stream().map(Mission::id).toList();
        };

        return ids.stream().map(missions::get).filter(java.util.Objects::nonNull).toList();
    }

    public PassPlayer refreshed(Player player) {

        PassPlayer state = pass.player(player);
        long today = clock.epochDay();

        if (state.dailyMissionDay() != today) {
            state.setDailyRotation(today, pick(pool(MissionScope.DAILY), dailyCount, today));
        }

        String week = clock.weekKey();
        if (!week.equals(state.weeklyKey())) {
            state.setWeeklyRotation(week, pick(pool(MissionScope.WEEKLY), weeklyCount, week.hashCode()));
        }

        return state;
    }

    /** Suma avance a toda misión vigente que encaje. Sin temporada abierta no cuenta nada. */
    public void progress(Player player, MissionType type, String target, int amount) {

        if (pass.openSeason().isEmpty() || amount <= 0) {
            return;
        }

        PassPlayer state = refreshed(player);

        for (MissionScope scope : MissionScope.values()) {
            for (Mission mission : active(player, scope)) {

                if (!mission.matches(type, target == null ? "" : target) || state.isCompleted(mission.id())) {
                    continue;
                }

                int value = state.addProgress(mission.id(), amount);

                if (value >= mission.amount()) {
                    state.complete(mission.id());
                    lang.send(player, "missions.completed", "mission", mission.name(), "xp", mission.xp());
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
                    pass.addXp(player, mission.xp());
                }
            }
        }
    }

    /** Al empezar una temporada nueva, las misiones de temporada vuelven a cero. */
    public void resetSeasonMissions(PassPlayer state) {
        Set<String> seasonIds = pool(MissionScope.SEASON).stream().map(Mission::id).collect(Collectors.toSet());
        state.forget(seasonIds);
    }

    private List<Mission> pool(MissionScope scope) {
        return missions.values().stream().filter(m -> m.scope() == scope).toList();
    }

    /** Sorteo determinista: misma semilla, mismas misiones. */
    static List<String> pick(List<Mission> pool, int count, long seed) {

        List<String> ids = new ArrayList<>(pool.stream().map(Mission::id).toList());
        Collections.shuffle(ids, new Random(seed));
        return List.copyOf(ids.subList(0, Math.min(count, ids.size())));
    }

}
