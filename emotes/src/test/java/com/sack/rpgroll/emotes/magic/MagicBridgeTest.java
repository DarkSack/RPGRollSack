package com.sack.rpgroll.emotes.magic;

import com.sack.rpgroll.common.event.magic.SpellCastEvent;
import com.sack.rpgroll.common.event.magic.SpellChannelEndEvent;
import com.sack.rpgroll.common.event.magic.SpellChannelStartEvent;
import com.sack.rpgroll.emotes.EmotesSettings;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.engine.FakeEmoteEngine;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import com.sack.rpgroll.emotes.service.EmoteService;
import org.bukkit.entity.Player;
import org.bukkit.Input;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicBridgeTest {

    @TempDir
    Path tmp;

    private final UUID id = UUID.randomUUID();
    private Player player;
    private FakeEmoteEngine engine;
    private EmoteService service;
    private EmotesSettings settings;
    private final List<Runnable> scheduled = new ArrayList<>();
    private final List<Long> delays = new ArrayList<>();
    private MagicBridge bridge;

    @BeforeEach
    void setUp() throws Exception {
        Path folder = tmp.resolve("emotes");
        Files.createDirectories(folder);
        for (String n : List.of("conjurar", "conjurar_caminando", "canalizar", "fuego", "lock")) {
            Files.writeString(folder.resolve(n + ".yml"), "animation: " + n + "\n"
                    + (n.equals("canalizar") ? "loop: true\n" : "")
                    + (n.equals("lock") ? "unlock: locked\n" : ""));
        }
        Files.writeString(folder.resolve("meditar.yml"), "animation: meditar\nloop: true\n");
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(folder.toFile()), w -> { throw new AssertionError(w); });
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        engine = new FakeEmoteEngine();
        EmotePlayerStore store = new EmotePlayerStore(new File(tmp.toFile(), "data"), 7);
        settings = settings(true, Map.of("fuego", "fuego"));
        service = new EmoteService(catalog, store, engine, () -> settings, () -> 1_000_000L, new com.sack.rpgroll.emotes.service.FakeEmoteAudio());
        bridge = new MagicBridge(service, catalog, () -> settings, (r, t) -> {
            scheduled.add(r);
            delays.add(t);
        });
    }

    private static EmotesSettings settings(boolean enabled, Map<String, String> cast) {
        return new EmotesSettings(3, 8, 0.3, 0.05, 7, "rpgroll_emotes", enabled,
                "conjurar", cast, "canalizar", Map.of(), true, true, true);
    }

    private void cast(String school) {
        bridge.onCast(new SpellCastEvent(player, "bola", school, Set.of()));
    }

    @Test
    void castUsesSchoolMappingOrDefault() {
        assertEquals("fuego", bridge.chooseCast("FUEGO", 0));
        assertEquals("conjurar", bridge.chooseCast("hielo", 0));
        assertEquals("conjurar", bridge.chooseCast(null, 0));
    }

    @Test
    void walkingVariantWhenMovingAndExists() {
        assertEquals("conjurar_caminando", bridge.chooseCast("hielo", 0.06));
        assertEquals("conjurar", bridge.chooseCast("hielo", 0.04));
    }

    @Test
    void walkingVariantMissingFallsBack() {
        assertEquals("fuego", bridge.chooseCast("fuego", 0.5));
    }

    @Test
    void castUsesInputToPickWalking() {
        Input in = mock(Input.class);
        when(in.isForward()).thenReturn(true);
        when(player.getCurrentInput()).thenReturn(in);
        cast("hielo");
        assertEquals(List.of("play:rpgroll_emotes/conjurar_caminando:false"), engine.calls);
        assertTrue(service.session(id).orElseThrow().magic());
    }

    @Test
    void castIdleIsNotWalking() {
        Input in = mock(Input.class);
        when(player.getCurrentInput()).thenReturn(in);
        cast("hielo");
        assertEquals(List.of("play:rpgroll_emotes/conjurar:false"), engine.calls);
    }

    @Test
    void busyPlayerGetsNoCastNorChannel() {
        // Montado, planeando, nadando, durmiendo o muerto: el hechizo sale, pero sin emote.
        when(player.isInsideVehicle()).thenReturn(true);
        cast("hielo");
        bridge.onChannelStart(new SpellChannelStartEvent(player, "bola", "hielo", 40));
        when(player.isInsideVehicle()).thenReturn(false);
        when(player.isGliding()).thenReturn(true);
        cast("hielo");
        assertTrue(engine.calls.isEmpty());
        assertTrue(scheduled.isEmpty());
    }

    @Test
    void completedChannelThenCastLeavesConjurar() {
        bridge.onChannelStart(new SpellChannelStartEvent(player, "bola", "hielo", 40));
        bridge.onChannelEnd(new SpellChannelEndEvent(player, "bola", SpellChannelEndEvent.Reason.COMPLETED));
        cast("hielo");
        assertEquals("conjurar", service.session(id).orElseThrow().emoteId());
    }

    @Test
    void channelStartPlaysLoopAndEndStops() {
        bridge.onChannelStart(new SpellChannelStartEvent(player, "bola", "hielo", 40));
        assertEquals(List.of("play:rpgroll_emotes/canalizar:true"), engine.calls);
        assertEquals(List.of(60L), delays);
        bridge.onChannelEnd(new SpellChannelEndEvent(player, "bola", SpellChannelEndEvent.Reason.MOVED));
        assertTrue(service.session(id).isEmpty());
        assertEquals("stop", engine.calls.getLast());
    }

    @Test
    void safetyCapStopsOnlyTheSameChannel() {
        bridge.onChannelStart(new SpellChannelStartEvent(player, "bola", "hielo", 40));
        scheduled.getFirst().run();
        assertTrue(service.session(id).isEmpty());

        bridge.onChannelStart(new SpellChannelStartEvent(player, "bola", "hielo", 40));
        cast("hielo");
        int calls = engine.calls.size();
        scheduled.getLast().run();
        assertEquals(calls, engine.calls.size());
        assertTrue(service.session(id).isPresent());
    }

    @Test
    void castReplacesLoopAndLoopDoesNotResume() {
        service.force(player, "meditar");
        cast("hielo");
        assertEquals("conjurar", service.session(id).orElseThrow().emoteId());
        engine.fireEnd(id);
        assertTrue(service.session(id).isEmpty());
    }

    @Test
    void lateChannelEndDoesNotTouchCast() {
        bridge.onChannelStart(new SpellChannelStartEvent(player, "bola", "hielo", 40));
        cast("hielo");
        bridge.onChannelEnd(new SpellChannelEndEvent(player, "bola", SpellChannelEndEvent.Reason.COMPLETED));
        assertEquals("conjurar", service.session(id).orElseThrow().emoteId());
    }

    @Test
    void disabledBridgeDoesNothing() {
        settings = settings(false, Map.of());
        cast("hielo");
        bridge.onChannelStart(new SpellChannelStartEvent(player, "bola", "hielo", 40));
        assertTrue(engine.calls.isEmpty());
        assertTrue(scheduled.isEmpty());
    }

    @Test
    void magicIgnoresLockAndCooldown() {
        settings = settings(true, Map.of("hielo", "lock"));
        cast("hielo");
        cast("hielo");
        assertEquals("lock", service.session(id).orElseThrow().emoteId());
        assertEquals(2, engine.calls.stream().filter(c -> c.startsWith("play")).count());
    }
}
