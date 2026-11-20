package com.sack.rpgroll.emotes.service;

import com.sack.rpgroll.emotes.EmotesSettings;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.engine.FakeEmoteEngine;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import org.bukkit.Location;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmoteMusicTest {

    private static final PlayerState IDLE = new PlayerState(false, false, false, false, false);
    private static final String PLAY = "play:rpgroll_emotes:musica.baile1";
    private static final String STOP = "stop:rpgroll_emotes:musica.baile1";

    @TempDir
    Path tmp;

    private final UUID id = UUID.randomUUID();
    private Player player;
    private FakeEmoteEngine engine;
    private FakeEmoteAudio audio;
    private EmoteService service;

    @BeforeEach
    void setUp() throws Exception {
        Path folder = tmp.resolve("emotes");
        Files.createDirectories(folder);
        String music = "music: { sound: \"rpgroll_emotes:musica.baile1\", volume: 1.0, length: 12.5 }\n";
        Files.writeString(folder.resolve("baile.yml"), "animation: baile\nloop: true\n" + music);
        Files.writeString(folder.resolve("tema.yml"), "animation: tema\n" + music);
        Files.writeString(folder.resolve("mudo.yml"), "animation: mudo\nloop: true\n");
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(folder.toFile()), w -> { throw new AssertionError(w); });

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getLocation()).thenReturn(mock(Location.class));
        engine = new FakeEmoteEngine();
        audio = new FakeEmoteAudio();
        EmotesSettings settings = new EmotesSettings(3, 8, 0.3, 0.05, 7, "rpgroll_emotes", true,
                "conjurar", Map.of(), "canalizar", Map.of(), true, true, true);
        service = new EmoteService(catalog, new EmotePlayerStore(new File(tmp.toFile(), "data"), 7), engine,
                () -> settings, () -> 1_000_000L, audio);
    }

    @Test
    void launchWithMusicPlaysIt() {
        service.launch(player, "baile", IDLE, false);
        assertEquals(List.of(PLAY), audio.calls);
    }

    @Test
    void loopRepeatsEveryLength() {
        service.launch(player, "baile", IDLE, false);
        assertEquals(1, audio.tasks.size());
        assertEquals(250, audio.tasks.get(0).ticks);
        audio.fireLast();
        assertEquals(List.of(PLAY, PLAY), audio.calls);
        assertEquals(2, audio.tasks.size()); // se vuelve a programar
        assertEquals(250, audio.tasks.get(1).ticks);
    }

    @Test
    void nonLoopDoesNotRepeat() {
        service.launch(player, "tema", IDLE, false);
        assertEquals(List.of(PLAY), audio.calls);
        assertTrue(audio.tasks.isEmpty());
    }

    @Test
    void stopCutsMusicOnce() {
        service.launch(player, "baile", IDLE, false);
        service.stop(player);
        service.stop(player);
        assertEquals(List.of(PLAY, STOP), audio.calls);
        assertEquals(0, audio.pending());
    }

    @Test
    void endOfAnimationCutsMusic() {
        service.launch(player, "tema", IDLE, false);
        engine.fireEnd(id);
        assertEquals(List.of(PLAY, STOP), audio.calls);
    }

    @Test
    void endOfLoopCancelsRepeat() {
        service.launch(player, "baile", IDLE, false);
        engine.fireEnd(id);
        assertEquals(List.of(PLAY, STOP), audio.calls);
        assertEquals(0, audio.pending());
    }

    @Test
    void replacingEmoteCutsOldMusic() {
        service.launch(player, "baile", IDLE, false);
        service.force(player, "mudo");
        assertEquals(List.of(PLAY, STOP), audio.calls);
        assertEquals(0, audio.pending());
    }

    @Test
    void replacingWithMusicEmoteCutsThenPlays() {
        service.launch(player, "baile", IDLE, false);
        service.forceMagic(player, "tema");
        assertEquals(List.of(PLAY, STOP, PLAY), audio.calls);
    }

    @Test
    void clearCutsMusic() {
        service.launch(player, "baile", IDLE, false);
        service.clear(id);
        assertEquals(List.of(PLAY, STOP), audio.calls);
        assertEquals(0, audio.pending());
    }

    @Test
    void lateRepeatAfterEndDoesNothing() {
        service.launch(player, "baile", IDLE, false);
        service.stop(player);
        audio.fireLast(); // el tick ya estaba en cola
        assertEquals(List.of(PLAY, STOP), audio.calls);
        assertEquals(0, audio.pending());
    }

    @Test
    void emoteWithoutMusicSchedulesNothing() {
        service.launch(player, "mudo", IDLE, false);
        service.stop(player);
        assertTrue(audio.calls.isEmpty());
        assertTrue(audio.tasks.isEmpty());
    }
}
