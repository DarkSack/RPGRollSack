package com.sack.rpgroll.emotes.service;

import com.sack.rpgroll.emotes.EmotesSettings;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.engine.FakeEmoteEngine;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmoteServiceTest {

    private static final PlayerState IDLE = new PlayerState(false, false, false, false, false);

    @TempDir
    Path tmp;

    private final UUID id = UUID.randomUUID();
    private Player player;
    private FakeEmoteEngine engine;
    private EmotePlayerStore store;
    private EmoteService service;
    private long now = 1_000_000L;

    @BeforeEach
    void setUp() throws Exception {
        Path folder = tmp.resolve("emotes");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("wave.yml"), "animation: wave\n");
        Files.writeString(folder.resolve("dance.yml"), "animation: dance\nunlock: locked\n");
        Files.writeString(folder.resolve("sit.yml"), "animation: sit\nloop: true\nmodel: otro_modelo\n");
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(folder.toFile()), w -> { throw new AssertionError(w); });

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        engine = new FakeEmoteEngine();
        store = new EmotePlayerStore(new File(tmp.toFile(), "data"), 7);
        EmotesSettings settings = new EmotesSettings(3, 8, 0.3, 0.05, 7, "rpgroll_emotes", true,
                "conjurar", Map.of(), "canalizar", Map.of(), true, true, true);
        service = new EmoteService(catalog, store, engine, () -> settings, () -> now, new FakeEmoteAudio());
    }

    @Test
    void launchesFreeEmote() {
        assertEquals(LaunchResult.OK, service.launch(player, "wave", IDLE, false));
        assertEquals(List.of("play:rpgroll_emotes/wave:false"), engine.calls);
        EmoteSession s = service.session(id).orElseThrow();
        assertEquals("wave", s.emoteId());
        assertFalse(s.loop());
        assertFalse(s.magic());
        assertEquals(now, s.startMillis());
        assertEquals(now, store.get(id).lastUseMillis());
    }

    @Test
    void usesEmoteModel() {
        assertEquals(LaunchResult.OK, service.launch(player, "SIT", IDLE, false));
        assertEquals(List.of("play:otro_modelo/sit:true"), engine.calls);
        assertTrue(service.session(id).orElseThrow().loop());
    }

    @Test
    void unknownEmoteFails() {
        assertEquals(LaunchResult.UNKNOWN, service.launch(player, "nada", IDLE, false));
        assertTrue(engine.calls.isEmpty());
    }

    @Test
    void lockedWithoutUnlockFails() {
        assertEquals(LaunchResult.LOCKED, service.launch(player, "dance", IDLE, false));
        assertTrue(engine.calls.isEmpty());
        assertTrue(service.session(id).isEmpty());
    }

    @Test
    void unlockedLockedEmoteLaunches() {
        store.get(id).unlock("dance");
        assertEquals(LaunchResult.OK, service.launch(player, "dance", IDLE, false));
    }

    @Test
    void permissionUnlocks() {
        when(player.hasPermission("rpgrollemotes.emote.dance")).thenReturn(true);
        assertEquals(LaunchResult.OK, service.launch(player, "dance", IDLE, false));

        Player p2 = mock(Player.class);
        when(p2.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p2.hasPermission("rpgrollemotes.emote.*")).thenReturn(true);
        assertEquals(LaunchResult.OK, service.launch(p2, "dance", IDLE, false));

        Player p3 = mock(Player.class);
        when(p3.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p3.hasPermission("rpgrollemotes.admin")).thenReturn(true);
        assertEquals(LaunchResult.OK, service.launch(p3, "dance", IDLE, false));
    }

    @Test
    void cooldownBlocksSecondLaunchWithin3s() {
        assertEquals(LaunchResult.OK, service.launch(player, "wave", IDLE, false));
        now += 2_999;
        assertEquals(LaunchResult.COOLDOWN, service.launch(player, "wave", IDLE, false));
        now += 1;
        assertEquals(LaunchResult.OK, service.launch(player, "wave", IDLE, false));
    }

    @Test
    void cooldownBypassDoesNotBlock() {
        assertEquals(LaunchResult.OK, service.launch(player, "wave", IDLE, false));
        now += 100;
        assertEquals(LaunchResult.OK, service.launch(player, "wave", IDLE, true));
    }

    @Test
    void busyStatesFail() {
        List<PlayerState> busy = List.of(
                new PlayerState(true, false, false, false, false),
                new PlayerState(false, true, false, false, false),
                new PlayerState(false, false, true, false, false),
                new PlayerState(false, false, false, true, false),
                new PlayerState(false, false, false, false, true));
        for (PlayerState state : busy) {
            assertEquals(LaunchResult.BUSY_STATE, service.launch(player, "wave", state, true), state.toString());
        }
        assertTrue(engine.calls.isEmpty());
    }

    @Test
    void combatTagBlocksFor8s() {
        service.markCombat(id, now);
        now += 7_999;
        assertEquals(LaunchResult.IN_COMBAT, service.launch(player, "wave", IDLE, true));
        now += 1;
        assertEquals(LaunchResult.OK, service.launch(player, "wave", IDLE, true));
    }

    @Test
    void newEmoteReplacesRunning() {
        assertEquals(LaunchResult.OK, service.launch(player, "sit", IDLE, false));
        now += 5_000;
        assertEquals(LaunchResult.OK, service.launch(player, "wave", IDLE, false));
        assertEquals(List.of("play:otro_modelo/sit:true", "stop", "play:rpgroll_emotes/wave:false"), engine.calls);
        // El onEnd del emote viejo no se lleva la sesión nueva.
        assertEquals("wave", service.session(id).orElseThrow().emoteId());
    }

    @Test
    void lateOnEndDoesNotClearNewSession() {
        // Un motor que no avisa al parar: el onEnd viejo llega tarde, con la sesión nueva ya puesta.
        Runnable[] first = new Runnable[1];
        FakeEmoteEngine lazy = new FakeEmoteEngine() {
            @Override
            public boolean play(Player p, String model, String animation, boolean loop, Runnable onEnd) {
                if (first[0] == null) {
                    first[0] = onEnd;
                }
                return super.play(p, model, animation, loop, onEnd);
            }

            @Override
            public void stop(Player p) {
                calls.add("stop");
            }
        };
        EmotesSettings settings = new EmotesSettings(3, 8, 0.3, 0.05, 7, "rpgroll_emotes", true,
                "conjurar", Map.of(), "canalizar", Map.of(), true, true, true);
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(tmp.resolve("emotes").toFile()), w -> { });
        EmoteService s = new EmoteService(catalog, store, lazy, () -> settings, () -> now, new FakeEmoteAudio());

        s.launch(player, "sit", IDLE, true);
        s.launch(player, "wave", IDLE, true);
        first[0].run();
        assertEquals("wave", s.session(id).orElseThrow().emoteId());
    }

    @Test
    void onEndClearsSession() {
        service.launch(player, "wave", IDLE, false);
        engine.fireEnd(id);
        assertTrue(service.session(id).isEmpty());
    }

    @Test
    void trackerClosedExternallyEndsSession() {
        // Bucle cerrado por fuera (/bm reload): el siguiente emote arranca sin parar nada que ya no existe.
        service.launch(player, "sit", IDLE, false);
        engine.fireEnd(id);
        assertTrue(service.session(id).isEmpty());
        assertEquals(LaunchResult.OK, service.launch(player, "wave", IDLE, true));
        assertEquals(List.of("play:otro_modelo/sit:true", "play:rpgroll_emotes/wave:false"), engine.calls);
    }

    @Test
    void engineFailureKeepsRunningEmote() {
        service.launch(player, "sit", IDLE, false);
        engine.existsResult = false;
        assertEquals(LaunchResult.ENGINE_FAILED, service.launch(player, "wave", IDLE, true));
        assertEquals("sit", service.session(id).orElseThrow().emoteId());
        assertEquals(List.of("play:otro_modelo/sit:true"), engine.calls);
        assertEquals(now, store.get(id).lastUseMillis());
    }

    @Test
    void favoriteSlotLaunches() {
        store.get(id).toggleFavorite("wave");
        assertEquals(LaunchResult.OK, service.launch(player, "1", IDLE, false));
        assertEquals("wave", service.session(id).orElseThrow().emoteId());
    }

    @Test
    void emptyFavoriteSlotFails() {
        assertEquals(LaunchResult.EMPTY_FAVORITE, service.launch(player, "5", IDLE, false));
        assertTrue(engine.calls.isEmpty());
        assertTrue(service.session(id).isEmpty());
    }

    @Test
    void engineFailureIsReported() {
        engine.playResult = false;
        assertEquals(LaunchResult.ENGINE_FAILED, service.launch(player, "wave", IDLE, false));
        assertTrue(service.session(id).isEmpty());
        assertEquals(0L, store.get(id).lastUseMillis());
    }

    @Test
    void forceIgnoresLocksAndCooldown() {
        service.markCombat(id, now);
        assertEquals(LaunchResult.OK, service.force(player, "dance"));
        assertEquals(LaunchResult.OK, service.force(player, "dance"));
        assertEquals("dance", service.session(id).orElseThrow().emoteId());
        assertEquals(LaunchResult.UNKNOWN, service.force(player, "nada"));
        // No cuenta como uso: el enfriamiento del jugador queda intacto.
        assertEquals(0L, store.get(id).lastUseMillis());
    }

    @Test
    void stopEndsSession() {
        service.launch(player, "sit", IDLE, false);
        service.stop(player);
        assertTrue(service.session(id).isEmpty());
        assertEquals("stop", engine.calls.getLast());
    }

    @Test
    void quitClosesSession() {
        service.launch(player, "sit", IDLE, false);
        service.markCombat(id, now);
        service.clear(id);
        assertTrue(service.session(id).isEmpty());
        assertEquals("stop", engine.calls.getLast());
        // También olvida la marca de combate.
        assertEquals(LaunchResult.OK, service.launch(player, "sit", IDLE, true));
    }

    @Test
    void forceMagicMarksSessionAndSkipsRules() {
        service.markCombat(id, now);
        store.get(id).lastUseMillis(now);
        assertEquals(LaunchResult.OK, service.forceMagic(player, "dance"));
        assertTrue(service.session(id).orElseThrow().magic());
        assertEquals(now, store.get(id).lastUseMillis());
        assertEquals(LaunchResult.UNKNOWN, service.forceMagic(player, "nada"));
    }
}
