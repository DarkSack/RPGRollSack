package com.sack.rpgroll.dungeons.engine;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.dungeons.core.DungeonAction;
import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonDifficulty;
import com.sack.rpgroll.dungeons.core.DungeonLootEntry;
import com.sack.rpgroll.dungeons.core.DungeonManager;
import com.sack.rpgroll.dungeons.core.DungeonModifierType;
import com.sack.rpgroll.dungeons.core.DungeonObjective;
import com.sack.rpgroll.dungeons.core.DungeonObjectiveType;
import com.sack.rpgroll.dungeons.core.DungeonRoom;
import com.sack.rpgroll.dungeons.core.DungeonTrigger;
import com.sack.rpgroll.dungeons.core.LootScope;
import com.sack.rpgroll.dungeons.integration.ItemsIntegration;
import com.sack.rpgroll.dungeons.integration.MobsIntegration;
import com.sack.rpgroll.dungeons.integration.QuestsIntegration;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.dungeons.player.DungeonPlayerStateManager;
import com.sack.rpgroll.dungeons.ranking.DungeonRankingManager;
import com.sack.rpgroll.dungeons.ranking.DungeonRunResult;
import com.sack.rpgroll.dungeons.registry.ActionRegistry;
import com.sack.rpgroll.dungeons.registry.DungeonActionContext;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquesta el ciclo de vida completo de una corrida: entrar (o encolarse
 * si la dungeon está ocupada), avanzar de sala en sala, oleadas, jefes,
 * objetivos, muerte/revivir, y el cierre final (loot, cooldown, ranking).
 * Es el único lugar que muta {@link DungeonSession} — listeners y
 * comandos solo llaman a estos métodos.
 */
public class DungeonEngine {

    private final org.bukkit.plugin.Plugin plugin;
    private final DungeonManager dungeonManager;
    private final DungeonPlayerStateManager stateManager;
    private final DungeonRankingManager rankingManager;
    private final ActionRegistry actionRegistry;

    private final Map<String, DungeonSession> activeSessions = new HashMap<>();
    private final Map<String, Deque<QueueEntry>> waitingQueues = new HashMap<>();

    private record QueueEntry(Team team, String difficultyId) {
    }

    public enum EnterResult {
        OK_STARTED,
        OK_QUEUED,
        ALREADY_RUNNING,
        PARTY_TOO_SMALL,
        PARTY_TOO_LARGE,
        ON_COOLDOWN,
        DIFFICULTY_NOT_FOUND
    }

    public DungeonEngine(org.bukkit.plugin.Plugin plugin, DungeonManager dungeonManager,
            DungeonPlayerStateManager stateManager, DungeonRankingManager rankingManager,
            ActionRegistry actionRegistry) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.stateManager = stateManager;
        this.rankingManager = rankingManager;
        this.actionRegistry = actionRegistry;

        actionRegistry.register("PROGRESS_OBJECTIVE", (action, ctx) -> {

            DungeonSession session = ctx.sessionId() != null ? findSessionById(ctx.sessionId()) : null;
            if (session == null) {
                return;
            }

            DungeonObjectiveType type;
            try {
                type = DungeonObjectiveType.valueOf(action.param("objective", "").toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return;
            }

            int amount = Integer.parseInt(action.param("amount", "1"));
            progressObjective(session, ctx.definition(), type, Map.of(), amount);
        });
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public DungeonPlayerStateManager getStateManager() {
        return stateManager;
    }

    public DungeonRankingManager getRankingManager() {
        return rankingManager;
    }

    public Optional<DungeonSession> getSession(String dungeonId) {
        return Optional.ofNullable(activeSessions.get(dungeonId.toLowerCase()));
    }

    public Collection<DungeonSession> getActiveSessions() {
        return activeSessions.values();
    }

    private DungeonSession findSessionById(UUID sessionId) {
        return activeSessions.values().stream().filter(s -> s.id().equals(sessionId)).findFirst().orElse(null);
    }

    public Optional<DungeonSession> findSessionByEntity(UUID entityId) {
        return activeSessions.values().stream()
                .filter(session -> session.spawnedEntityIds().contains(entityId))
                .findFirst();
    }

    public Optional<DungeonSession> findSessionByPlayer(UUID playerId) {
        return activeSessions.values().stream()
                .filter(session -> session.partyMemberIds().contains(playerId))
                .findFirst();
    }

    // ============ Entrada / cola ============

    public EnterResult tryEnter(Team team, DungeonDefinition definition, String difficultyId) {

        if (team.size() < definition.minPlayers()) {
            return EnterResult.PARTY_TOO_SMALL;
        }

        if (team.size() > definition.maxPlayers()) {
            return EnterResult.PARTY_TOO_LARGE;
        }

        DungeonDifficulty difficulty = definition.difficulty(difficultyId).orElse(null);
        if (difficulty == null) {
            return EnterResult.DIFFICULTY_NOT_FOUND;
        }

        long now = System.currentTimeMillis();
        for (UUID memberId : team.members()) {

            long completedAt = stateManager.getOrLoad(memberId).getCompletedAt(definition.id());
            if (completedAt <= 0) {
                continue;
            }

            if (!definition.repeatable() || (definition.cooldownMillis() > 0
                    && now - completedAt < definition.cooldownMillis())) {
                return EnterResult.ON_COOLDOWN;
            }
        }

        if (activeSessions.containsKey(definition.id().toLowerCase())) {

            if (findSessionByPlayer(team.leaderId()).isPresent()) {
                return EnterResult.ALREADY_RUNNING;
            }

            waitingQueues.computeIfAbsent(definition.id().toLowerCase(), key -> new ArrayDeque<>())
                    .add(new QueueEntry(team, difficultyId));
            return EnterResult.OK_QUEUED;
        }

        startSession(definition, difficulty, team);
        return EnterResult.OK_STARTED;
    }

    private void startSession(DungeonDefinition definition, DungeonDifficulty difficulty, Team team) {

        DungeonSession session = new DungeonSession(definition.id(), difficulty, new ArrayList<>(team.members()));
        activeSessions.put(definition.id().toLowerCase(), session);

        if (definition.checkpointPolicy().sharedLives() && !definition.checkpointPolicy().unlimitedRetries()) {
            session.setSharedLivesRemaining(definition.checkpointPolicy().maxRetries());
        }

        for (UUID memberId : session.partyMemberIds()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                session.rememberReturnLocation(player);
                player.teleport(definition.lobbyPoint().toLocation());
            }
        }

        fireDefinitionTrigger(definition, session, DungeonTrigger.DUNGEON_START, null, null);
        enterRoomAt(session, definition, 0);
    }

    // ============ Salas ============

    private void enterRoomAt(DungeonSession session, DungeonDefinition definition, int roomIndex) {

        Optional<DungeonRoom> roomOpt = definition.roomAt(roomIndex);

        if (roomOpt.isEmpty()) {
            completeSession(session, definition, DungeonSessionState.COMPLETED);
            return;
        }

        DungeonRoom room = roomOpt.get();
        session.enterRoom(roomIndex);
        session.setCheckpointRoomIndex(roomIndex);

        Location entryLocation = room.entryPoint().toLocation();
        for (Player player : onlinePartyMembers(session)) {
            player.teleport(entryLocation);
        }

        fireDefinitionTrigger(definition, session, DungeonTrigger.ROOM_ENTER, room, null);
        fireRoomTrigger(definition, session, room, DungeonTrigger.ROOM_ENTER, null);

        if (room.hasBoss()) {
            spawnBoss(session, definition, room);
        }

        if (!room.waves().isEmpty()) {
            startWave(session, definition, room, 0);
        } else {
            checkRoomCompletion(session, definition, room);
        }
    }

    private void spawnBoss(DungeonSession session, DungeonDefinition definition, DungeonRoom room) {

        MobsIntegration.spawnMob(room.bossMobId(), room.entryPoint().toLocation()).ifPresent(entity -> {
            applyDifficultyHealth(entity, session.difficulty());
            session.trackSpawnedEntity(entity.getUniqueId());
        });

        fireDefinitionTrigger(definition, session, DungeonTrigger.BOSS_SPAWN, room, null);
        fireRoomTrigger(definition, session, room, DungeonTrigger.BOSS_SPAWN, null);
    }

    private void startWave(DungeonSession session, DungeonDefinition definition, DungeonRoom room, int waveIndex) {

        if (waveIndex >= room.waves().size()) {
            checkRoomCompletion(session, definition, room);
            return;
        }

        var wave = room.waves().get(waveIndex);
        Location spawnLocation = room.entryPoint().toLocation();

        int spawned = 0;
        for (var waveMob : wave.mobs()) {
            for (int i = 0; i < waveMob.amount(); i++) {

                Optional<org.bukkit.entity.LivingEntity> entity = MobsIntegration.spawnMob(waveMob.mobId(),
                        spawnLocation);

                if (entity.isPresent()) {
                    applyDifficultyHealth(entity.get(), session.difficulty());
                    session.trackSpawnedEntity(entity.get().getUniqueId());
                    spawned++;
                }
            }
        }

        session.startWave(waveIndex, spawned);

        fireDefinitionTrigger(definition, session, DungeonTrigger.WAVE_START, room, null);
        fireRoomTrigger(definition, session, room, DungeonTrigger.WAVE_START, null);
    }

    private void applyDifficultyHealth(LivingEntity entity, DungeonDifficulty difficulty) {

        if (difficulty.healthMultiplier() == 1.0) {
            return;
        }

        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        attribute.setBaseValue(attribute.getBaseValue() * difficulty.healthMultiplier());
        entity.setHealth(attribute.getValue());
    }

    /** Multiplicador de daño de dificultad para el daño que un mob de esta sesión inflige a un jugador. */
    public double outgoingDamageMultiplier(UUID entityId) {
        return findSessionByEntity(entityId).map(session -> session.difficulty().damageMultiplier()).orElse(1.0);
    }

    public void onMobDeath(UUID entityId) {

        Optional<DungeonSession> sessionOpt = findSessionByEntity(entityId);
        if (sessionOpt.isEmpty()) {
            return;
        }

        DungeonSession session = sessionOpt.get();
        session.untrackEntity(entityId);

        DungeonDefinition definition = dungeonManager.get(session.dungeonId()).orElse(null);
        if (definition == null) {
            return;
        }

        Optional<DungeonRoom> roomOpt = definition.roomAt(session.currentRoomIndex());
        if (roomOpt.isEmpty()) {
            return;
        }

        DungeonRoom room = roomOpt.get();

        if (session.activeWaveIndex() >= 0 && session.waveMobsRemaining() > 0) {

            session.decrementWaveMobsRemaining();

            if (session.waveMobsRemaining() == 0) {
                fireDefinitionTrigger(definition, session, DungeonTrigger.WAVE_CLEAR, room, null);
                fireRoomTrigger(definition, session, room, DungeonTrigger.WAVE_CLEAR, null);
                startWave(session, definition, room, session.activeWaveIndex() + 1);
            }

            return;
        }

        if (room.hasBoss() && session.currentRoomEntityIds().isEmpty()) {
            fireDefinitionTrigger(definition, session, DungeonTrigger.BOSS_DEFEATED, room, null);
            fireRoomTrigger(definition, session, room, DungeonTrigger.BOSS_DEFEATED, null);
        }

        checkRoomCompletion(session, definition, room);
    }

    // ============ Objetivos ============

    public void progressObjective(DungeonSession session, DungeonDefinition definition, DungeonObjectiveType type,
            Map<String, String> eventParams, int amount) {

        Optional<DungeonRoom> roomOpt = definition.roomAt(session.currentRoomIndex());
        if (roomOpt.isEmpty()) {
            return;
        }

        DungeonRoom room = roomOpt.get();
        List<DungeonObjective> objectives = room.objectives();

        for (int i = 0; i < objectives.size(); i++) {

            DungeonObjective objective = objectives.get(i);

            if (objective.type() != type || !matchesParams(objective, eventParams)) {
                continue;
            }

            session.addObjectiveProgress(i, amount);
        }

        checkRoomCompletion(session, definition, room);
    }

    private boolean matchesParams(DungeonObjective objective, Map<String, String> eventParams) {

        for (var entry : objective.params().entrySet()) {

            if (entry.getKey().equals("amount") || entry.getKey().equals("description")) {
                continue;
            }

            String expected = entry.getValue();
            String actual = eventParams.get(entry.getKey());

            if (actual != null && !actual.equalsIgnoreCase(expected)) {
                return false;
            }
        }

        return true;
    }

    private void checkRoomCompletion(DungeonSession session, DungeonDefinition definition, DungeonRoom room) {

        if (!session.currentRoomEntityIds().isEmpty()) {
            return;
        }

        List<DungeonObjective> objectives = room.objectives();

        for (int i = 0; i < objectives.size(); i++) {
            if (session.getObjectiveProgress(i) < objectives.get(i).amount()) {
                return;
            }
        }

        fireDefinitionTrigger(definition, session, DungeonTrigger.ROOM_CLEAR, room, null);
        fireRoomTrigger(definition, session, room, DungeonTrigger.ROOM_CLEAR, null);

        if (definition.isLastRoom(session.currentRoomIndex())) {
            completeSession(session, definition, DungeonSessionState.COMPLETED);
        } else {
            enterRoomAt(session, definition, session.currentRoomIndex() + 1);
        }
    }

    // ============ Muerte / revivir / wipe ============

    public void onPlayerDeath(Player player, DungeonSession session, DungeonDefinition definition) {

        session.markDead(player.getUniqueId());
        fireDefinitionTrigger(definition, session, DungeonTrigger.PLAYER_DEATH, null, player);

        var policy = definition.checkpointPolicy();

        if (policy.sharedLives() && session.sharedLivesRemaining() >= 0) {

            session.decrementSharedLives();

            if (session.sharedLivesRemaining() <= 0) {
                completeSession(session, definition, DungeonSessionState.FAILED);
                return;
            }
        }

        if (session.isFullyWiped()) {
            handleWipe(session, definition);
        }
    }

    public boolean revive(DungeonSession session, DungeonDefinition definition, UUID targetId) {

        if (!session.deadPlayers().contains(targetId)) {
            return false;
        }

        session.markRevived(targetId);

        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            target.spigot().respawn();
            definition.roomAt(session.currentRoomIndex())
                    .ifPresent(room -> target.teleport(room.entryPoint().toLocation()));
            fireDefinitionTrigger(definition, session, DungeonTrigger.PLAYER_REVIVE, null, target);
        }

        return true;
    }

    private void handleWipe(DungeonSession session, DungeonDefinition definition) {

        var policy = definition.checkpointPolicy();

        // Si las vidas son compartidas, maxRetries ya se consume como pool de
        // vidas en onPlayerDeath (una FAILED ahí corta antes de llegar acá) —
        // este chequeo de reintentos es la variante "sin vida compartida".
        if (!policy.sharedLives() && !policy.unlimitedRetries() && session.retriesUsed() >= policy.maxRetries()) {
            completeSession(session, definition, DungeonSessionState.FAILED);
            return;
        }

        session.incrementRetries();

        int targetRoomIndex = switch (policy.mode()) {
            case RESTART_DUNGEON -> 0;
            case RESTART_ROOM -> session.currentRoomIndex();
            case REVIVE_AT_CHECKPOINT -> session.checkpointRoomIndex();
        };

        for (UUID memberId : session.deadPlayers()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                player.spigot().respawn();
            }
        }
        session.deadPlayers().clear();

        for (Player player : onlinePartyMembers(session)) {
            player.setHealth(Optional.ofNullable(player.getAttribute(Attribute.MAX_HEALTH))
                    .map(AttributeInstance::getValue).orElse(20.0));
        }

        enterRoomAt(session, definition, targetRoomIndex);
    }

    // ============ Cierre ============

    public void abandon(DungeonSession session, DungeonDefinition definition) {
        completeSession(session, definition, DungeonSessionState.ABANDONED);
    }

    private void completeSession(DungeonSession session, DungeonDefinition definition, DungeonSessionState result) {

        session.setState(result);

        DungeonTrigger trigger = switch (result) {
            case COMPLETED -> DungeonTrigger.DUNGEON_COMPLETE;
            case FAILED -> DungeonTrigger.DUNGEON_FAIL;
            case ABANDONED -> DungeonTrigger.DUNGEON_ABANDON;
            case RUNNING -> null;
        };

        if (trigger != null) {
            fireDefinitionTrigger(definition, session, trigger, null, null);
        }

        cleanupEntities(session);

        if (result == DungeonSessionState.COMPLETED) {
            grantLoot(definition, session);
            long now = System.currentTimeMillis();
            List<String> names = new ArrayList<>();

            for (UUID memberId : session.partyMemberIds()) {
                var state = stateManager.getOrLoad(memberId);
                state.markCompleted(definition.id(), now);
                stateManager.save(memberId);

                Player player = Bukkit.getPlayer(memberId);
                names.add(player != null ? player.getName() : memberId.toString());
            }

            rankingManager.recordRun(new DungeonRunResult(definition.id(), session.difficulty().id(), names, now,
                    now - session.startedAtMillis(), session.deathCount(), session.totalDamage()));
        }

        for (UUID memberId : session.partyMemberIds()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player == null) {
                continue;
            }
            Location returnLocation = session.returnLocation(memberId);
            if (returnLocation != null) {
                player.teleport(returnLocation);
            }
        }

        activeSessions.remove(definition.id().toLowerCase());
        promoteNextQueued(definition);
    }

    private void promoteNextQueued(DungeonDefinition definition) {

        Deque<QueueEntry> queue = waitingQueues.get(definition.id().toLowerCase());
        if (queue == null || queue.isEmpty()) {
            return;
        }

        QueueEntry next = queue.poll();
        DungeonDifficulty difficulty = definition.difficulty(next.difficultyId())
                .orElse(definition.difficulties().get(0));

        for (UUID memberId : next.team().members()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                player.sendMessage(Component.text("¡Es el turno de tu grupo para entrar a ", NamedTextColor.GREEN)
                        .append(ComponentUtils.parse(definition.displayName()))
                        .append(Component.text("!", NamedTextColor.GREEN)));
            }
        }

        startSession(definition, difficulty, next.team());
    }

    private void cleanupEntities(DungeonSession session) {

        for (UUID entityId : List.copyOf(session.spawnedEntityIds())) {
            var entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }

        session.spawnedEntityIds().clear();
        session.currentRoomEntityIds().clear();
    }

    // ============ Loot ============

    private void grantLoot(DungeonDefinition definition, DungeonSession session) {

        if (definition.loot().isEmpty()) {
            return;
        }

        List<Player> online = onlinePartyMembers(session);
        if (online.isEmpty()) {
            return;
        }

        for (DungeonLootEntry entry : definition.loot()) {

            if (entry.scope() == LootScope.PER_PLAYER) {
                online.forEach(player -> rollAndGrant(entry, player));
                continue;
            }

            if (entry.scope() == LootScope.PER_CONTRIBUTION && !session.damageContribution().isEmpty()) {
                session.damageContribution().entrySet().stream()
                        .max(Comparator.comparingDouble(Map.Entry::getValue))
                        .map(Map.Entry::getKey)
                        .map(Bukkit::getPlayer)
                        .ifPresentOrElse(player -> rollAndGrant(entry, player),
                                () -> rollAndGrant(entry, online.get(0)));
                continue;
            }

            rollAndGrant(entry, online.get(0));
        }
    }

    private void rollAndGrant(DungeonLootEntry entry, Player player) {

        if (player == null || Math.random() * 100 >= entry.chance()) {
            return;
        }

        int amount = entry.amountMin() == entry.amountMax()
                ? entry.amountMin()
                : entry.amountMin() + (int) (Math.random() * (entry.amountMax() - entry.amountMin() + 1));

        switch (entry.type()) {
            case ITEM -> ItemsIntegration.giveItem(player, entry.reference(), amount);
            case MONEY -> giveMoney(player, amount);
            case EXPERIENCE -> player.giveExp(amount);
            case COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    entry.reference().replace("{player}", player.getName()));
            case QUEST -> QuestsIntegration.startQuest(player, entry.reference());
        }
    }

    private void giveMoney(Player player, double amount) {

        if (!RPGRollAPI.isReady()) {
            return;
        }

        RPGRollAPI.get().getEconomyProvider().getEconomy()
                .ifPresent(economy -> economy.depositPlayer(player, amount));
    }

    // ============ Triggers ============

    private void fireDefinitionTrigger(DungeonDefinition definition, DungeonSession session, DungeonTrigger trigger,
            DungeonRoom room, Player target) {

        List<DungeonAction> actions = definition.actionsFor(trigger);
        if (actions.isEmpty()) {
            return;
        }

        actionRegistry.executeAll(actions,
                new DungeonActionContext(definition, session.id(), onlinePartyMembers(session), target, room));
    }

    private void fireRoomTrigger(DungeonDefinition definition, DungeonSession session, DungeonRoom room,
            DungeonTrigger trigger, Player target) {

        List<DungeonAction> actions = room.eventsFor(trigger);
        if (actions.isEmpty()) {
            return;
        }

        actionRegistry.executeAll(actions,
                new DungeonActionContext(definition, session.id(), onlinePartyMembers(session), target, room));
    }

    // ============ Helpers ============

    private List<Player> onlinePartyMembers(DungeonSession session) {

        List<Player> players = new ArrayList<>();

        for (UUID memberId : session.partyMemberIds()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                players.add(player);
            }
        }

        return players;
    }

    /** Llamado periódicamente por {@code DungeonSessionTask} — timers de sala/oleada y objetivos por tiempo. */
    public void tick() {

        for (DungeonSession session : List.copyOf(activeSessions.values())) {

            DungeonDefinition definition = dungeonManager.get(session.dungeonId()).orElse(null);
            if (definition == null) {
                continue;
            }

            Optional<DungeonRoom> roomOpt = definition.roomAt(session.currentRoomIndex());
            if (roomOpt.isEmpty()) {
                continue;
            }

            DungeonRoom room = roomOpt.get();

            tickTimeObjectives(session, definition, room);
            tickWaveTimeout(session, definition, room);
            tickRevive(session, definition);
            applyModifierTicks(session);
        }
    }

    private void tickTimeObjectives(DungeonSession session, DungeonDefinition definition, DungeonRoom room) {

        boolean hasTimeObjective = room.objectives().stream()
                .anyMatch(o -> o.type() == DungeonObjectiveType.SURVIVE_TIME
                        || o.type() == DungeonObjectiveType.RESIST_TIME);

        if (!hasTimeObjective || session.deadPlayers().size() == session.partySize()) {
            return;
        }

        progressObjective(session, definition, DungeonObjectiveType.SURVIVE_TIME, Map.of(), 1);
        progressObjective(session, definition, DungeonObjectiveType.RESIST_TIME, Map.of(), 1);
    }

    private void tickWaveTimeout(DungeonSession session, DungeonDefinition definition, DungeonRoom room) {

        if (session.activeWaveIndex() < 0 || session.activeWaveIndex() >= room.waves().size()) {
            return;
        }

        var wave = room.waves().get(session.activeWaveIndex());
        if (wave.timeLimitMillis() <= 0) {
            return;
        }

        if (System.currentTimeMillis() - session.waveStartedAtMillis() > wave.timeLimitMillis()) {
            handleWipe(session, definition);
        }
    }

    private void tickRevive(DungeonSession session, DungeonDefinition definition) {

        if (definition.reviveConfig().mode() != com.sack.rpgroll.dungeons.core.ReviveMode.AFTER_TIMER) {
            return;
        }

        long timerMillis = definition.reviveConfig().reviveTimerMillis();

        for (UUID deadId : List.copyOf(session.deadPlayers())) {
            if (System.currentTimeMillis() - session.deathTimestamp(deadId) >= timerMillis) {
                revive(session, definition, deadId);
            }
        }
    }

    private void applyModifierTicks(DungeonSession session) {

        if (!session.difficulty().hasModifier(DungeonModifierType.PERMANENT_DARKNESS)) {
            return;
        }

        for (Player player : onlinePartyMembers(session)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false));
        }
    }

}
