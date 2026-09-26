package com.sack.rpgroll.pass.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estado de un jugador en el pase, el diario y los votos. Mutable y solo se
 * toca desde el hilo principal; {@link PassPlayerStore} lo guarda cuando está
 * marcado como sucio.
 */
public class PassPlayer {

    private final UUID uuid;

    // Pase de la temporada.
    String seasonId = "";
    int xp;
    final Set<Integer> claimedFree = new HashSet<>();
    final Set<Integer> claimedPremium = new HashSet<>();
    // Para los requisitos de nivel: minutos jugados sin AFK y misiones completadas en la temporada.
    int seasonPlaytime;
    int seasonMissions;

    // Misiones: las sorteadas de hoy y de esta semana, y el avance de todas.
    long dailyMissionDay = Long.MIN_VALUE;
    final List<String> dailyMissions = new ArrayList<>();
    String weeklyKey = "";
    final List<String> weeklyMissions = new ArrayList<>();
    final Map<String, Integer> progress = new HashMap<>();
    final Set<String> completed = new HashSet<>();

    // Recompensa diaria.
    long lastDailyClaimDay = Long.MIN_VALUE;
    int dailyStreak;

    // Votos.
    int votesTotal;
    long lastVoteDay = Long.MIN_VALUE;
    int voteStreak;

    private boolean dirty;

    public PassPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public String seasonId() {
        return seasonId;
    }

    public int xp() {
        return xp;
    }

    public int seasonPlaytime() {
        return seasonPlaytime;
    }

    public int seasonMissions() {
        return seasonMissions;
    }

    public boolean hasClaimed(int level, boolean premium) {
        return (premium ? claimedPremium : claimedFree).contains(level);
    }

    public List<String> dailyMissions() {
        return List.copyOf(dailyMissions);
    }

    public long dailyMissionDay() {
        return dailyMissionDay;
    }

    public List<String> weeklyMissions() {
        return List.copyOf(weeklyMissions);
    }

    public String weeklyKey() {
        return weeklyKey;
    }

    public int progress(String missionId) {
        return progress.getOrDefault(missionId, 0);
    }

    public boolean isCompleted(String missionId) {
        return completed.contains(missionId);
    }

    public long lastDailyClaimDay() {
        return lastDailyClaimDay;
    }

    public int dailyStreak() {
        return dailyStreak;
    }

    public int votesTotal() {
        return votesTotal;
    }

    public long lastVoteDay() {
        return lastVoteDay;
    }

    public int voteStreak() {
        return voteStreak;
    }

    // ============ Cambios ============

    /** Nueva temporada: se pierde el avance del pase, no el diario ni los votos. */
    public void startSeason(String id) {
        seasonId = id;
        xp = 0;
        claimedFree.clear();
        claimedPremium.clear();
        seasonPlaytime = 0;
        seasonMissions = 0;
        markDirty();
    }

    public void addXp(int amount) {
        xp = Math.max(0, xp + amount);
        markDirty();
    }

    public void addPlaytime(int minutes) {
        seasonPlaytime += Math.max(0, minutes);
        markDirty();
    }

    public void markClaimed(int level, boolean premium) {
        (premium ? claimedPremium : claimedFree).add(level);
        markDirty();
    }

    public void setDailyRotation(long day, List<String> missions) {
        rotate(dailyMissions, missions);
        dailyMissionDay = day;
    }

    public void setWeeklyRotation(String key, List<String> missions) {
        rotate(weeklyMissions, missions);
        weeklyKey = key;
    }

    /** Suma avance y devuelve el nuevo valor. */
    public int addProgress(String missionId, int amount) {
        int value = progress.merge(missionId, amount, Integer::sum);
        markDirty();
        return value;
    }

    public void complete(String missionId) {
        if (completed.add(missionId)) {
            seasonMissions++;
        }
        markDirty();
    }

    /** Olvida el avance de unas misiones (las de temporada, al cambiar de temporada). */
    public void forget(Set<String> missionIds) {
        missionIds.forEach(progress::remove);
        completed.removeAll(missionIds);
        markDirty();
    }

    public void recordDailyClaim(long day, int streak) {
        lastDailyClaimDay = day;
        dailyStreak = streak;
        markDirty();
    }

    public void recordVote(long day, int streak) {
        votesTotal++;
        lastVoteDay = day;
        voteStreak = streak;
        markDirty();
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    void markClean() {
        dirty = false;
    }

    /** Sustituye un grupo de misiones sorteadas y olvida el avance de las que salen. */
    private void rotate(List<String> current, List<String> next) {
        for (String old : current) {
            progress.remove(old);
            completed.remove(old);
        }
        current.clear();
        current.addAll(next);
        markDirty();
    }

}
