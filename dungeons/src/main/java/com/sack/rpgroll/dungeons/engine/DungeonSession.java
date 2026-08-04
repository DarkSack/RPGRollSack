package com.sack.rpgroll.dungeons.engine;

import com.sack.rpgroll.dungeons.core.DungeonDifficulty;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estado en memoria de una corrida en curso. No persiste — si el server
 * se reinicia a mitad de una corrida, se pierde (aceptable: el grupo
 * vuelve a entrar y arranca de cero, el cooldown/ranking de corridas ya
 * completadas sí persiste vía {@code DungeonPlayerStateManager}/
 * {@code DungeonRankingManager}).
 */
public class DungeonSession {

    private final UUID id = UUID.randomUUID();
    private final String dungeonId;
    private final DungeonDifficulty difficulty;
    private final List<UUID> partyMemberIds;
    private final Map<UUID, Location> returnLocations = new LinkedHashMap<>();
    private final long startedAtMillis;

    private DungeonSessionState state = DungeonSessionState.RUNNING;
    private int currentRoomIndex = 0;
    private int checkpointRoomIndex = 0;
    private int retriesUsed = 0;
    private long roomEnteredAtMillis;

    private int activeWaveIndex = -1;
    private long waveStartedAtMillis;
    private int waveMobsRemaining = 0;

    private final Set<UUID> spawnedEntityIds = new HashSet<>();
    private final Set<UUID> currentRoomEntityIds = new HashSet<>();
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
    private final Map<UUID, Double> damageContribution = new HashMap<>();
    private final Set<UUID> deadPlayers = new HashSet<>();
    private final Map<UUID, Long> deathTimestamps = new HashMap<>();
    private int deathCount = 0;
    private int sharedLivesRemaining = -1;

    public DungeonSession(String dungeonId, DungeonDifficulty difficulty, List<UUID> partyMemberIds) {
        this.dungeonId = dungeonId;
        this.difficulty = difficulty;
        this.partyMemberIds = List.copyOf(partyMemberIds);
        this.startedAtMillis = System.currentTimeMillis();
        this.roomEnteredAtMillis = startedAtMillis;
    }

    public UUID id() {
        return id;
    }

    public String dungeonId() {
        return dungeonId;
    }

    public DungeonDifficulty difficulty() {
        return difficulty;
    }

    public List<UUID> partyMemberIds() {
        return partyMemberIds;
    }

    public int partySize() {
        return partyMemberIds.size();
    }

    public void rememberReturnLocation(Player player) {
        returnLocations.put(player.getUniqueId(), player.getLocation());
    }

    public Location returnLocation(UUID playerId) {
        return returnLocations.get(playerId);
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public DungeonSessionState state() {
        return state;
    }

    public void setState(DungeonSessionState state) {
        this.state = state;
    }

    public int currentRoomIndex() {
        return currentRoomIndex;
    }

    public void enterRoom(int roomIndex) {
        this.currentRoomIndex = roomIndex;
        this.roomEnteredAtMillis = System.currentTimeMillis();
        this.activeWaveIndex = -1;
        this.waveMobsRemaining = 0;
        this.objectiveProgress.clear();
        this.currentRoomEntityIds.clear();
    }

    public long roomEnteredAtMillis() {
        return roomEnteredAtMillis;
    }

    public int checkpointRoomIndex() {
        return checkpointRoomIndex;
    }

    public void setCheckpointRoomIndex(int checkpointRoomIndex) {
        this.checkpointRoomIndex = checkpointRoomIndex;
    }

    public int retriesUsed() {
        return retriesUsed;
    }

    public void incrementRetries() {
        retriesUsed++;
    }

    public int activeWaveIndex() {
        return activeWaveIndex;
    }

    public void startWave(int waveIndex, int mobCount) {
        this.activeWaveIndex = waveIndex;
        this.waveStartedAtMillis = System.currentTimeMillis();
        this.waveMobsRemaining = mobCount;
    }

    public long waveStartedAtMillis() {
        return waveStartedAtMillis;
    }

    public int waveMobsRemaining() {
        return waveMobsRemaining;
    }

    public void decrementWaveMobsRemaining() {
        waveMobsRemaining = Math.max(0, waveMobsRemaining - 1);
    }

    public Set<UUID> spawnedEntityIds() {
        return spawnedEntityIds;
    }

    public Set<UUID> currentRoomEntityIds() {
        return currentRoomEntityIds;
    }

    public void trackSpawnedEntity(UUID entityId) {
        spawnedEntityIds.add(entityId);
        currentRoomEntityIds.add(entityId);
    }

    public void untrackEntity(UUID entityId) {
        spawnedEntityIds.remove(entityId);
        currentRoomEntityIds.remove(entityId);
    }

    public int getObjectiveProgress(int objectiveIndex) {
        return objectiveProgress.getOrDefault(objectiveIndex, 0);
    }

    public void addObjectiveProgress(int objectiveIndex, int amount) {
        objectiveProgress.merge(objectiveIndex, amount, Integer::sum);
    }

    public void addDamageContribution(UUID playerId, double amount) {
        damageContribution.merge(playerId, amount, Double::sum);
    }

    public Map<UUID, Double> damageContribution() {
        return damageContribution;
    }

    public double totalDamage() {
        return damageContribution.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public Set<UUID> deadPlayers() {
        return deadPlayers;
    }

    public void markDead(UUID playerId) {
        deadPlayers.add(playerId);
        deathTimestamps.put(playerId, System.currentTimeMillis());
        deathCount++;
    }

    public void markRevived(UUID playerId) {
        deadPlayers.remove(playerId);
        deathTimestamps.remove(playerId);
    }

    public long deathTimestamp(UUID playerId) {
        return deathTimestamps.getOrDefault(playerId, 0L);
    }

    public boolean isFullyWiped() {
        return deadPlayers.size() >= partySize();
    }

    public int deathCount() {
        return deathCount;
    }

    public void setSharedLivesRemaining(int sharedLivesRemaining) {
        this.sharedLivesRemaining = sharedLivesRemaining;
    }

    public int sharedLivesRemaining() {
        return sharedLivesRemaining;
    }

    public void decrementSharedLives() {
        if (sharedLivesRemaining > 0) {
            sharedLivesRemaining--;
        }
    }

}
