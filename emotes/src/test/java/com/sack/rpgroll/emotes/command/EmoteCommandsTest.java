package com.sack.rpgroll.emotes.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.emotes.EmotesSettings;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.engine.FakeEmoteEngine;
import com.sack.rpgroll.emotes.player.EmotePlayer;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import com.sack.rpgroll.emotes.service.EmoteService;
import com.sack.rpgroll.emotes.service.LaunchResult;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmoteCommandsTest {

    @TempDir
    Path tmp;

    private final UUID id = UUID.randomUUID();
    private Player player;
    private LangManager lang;
    private FakeEmoteEngine engine;
    private EmotePlayerStore store;
    private EmoteCommand emote;
    private EmotesAdminCommand admin;
    private CommandSender console;
    private boolean reloaded;
    private final UUID offlineId = UUID.randomUUID();
    private OfflinePlayer offlinePlayer;
    private Path folder;
    private EmoteCatalog catalog;
    private EmoteService service;

    @BeforeEach
    void setUp() throws Exception {
        folder = tmp.resolve("emotes");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("wave.yml"), "animation: wave\n");
        Files.writeString(folder.resolve("dance.yml"), "animation: dance\nunlock: locked\n");
        Files.writeString(folder.resolve("secret.yml"), "animation: secret\nunlock: locked\nhidden: true\n");
        catalog = new EmoteCatalog();
        catalog.load(List.of(folder.toFile()), w -> { throw new AssertionError(w); });

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn("Sack");
        lang = mock(LangManager.class);
        engine = new FakeEmoteEngine();
        store = new EmotePlayerStore(new File(tmp.toFile(), "data"), 7);
        EmotesSettings settings = new EmotesSettings(3, 8, 0.3, 0.05, 7, "rpgroll_emotes", true,
                "conjurar", Map.of(), "canalizar", Map.of(), true, true, true);
        service = new EmoteService(catalog, store, engine, () -> settings, () -> 1_000_000L, new com.sack.rpgroll.emotes.service.FakeEmoteAudio());
        emote = new EmoteCommand(service, catalog, store, lang, p -> { });
        console = mock(CommandSender.class);
        offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getUniqueId()).thenReturn(offlineId);
        admin = new EmotesAdminCommand(service, catalog, store, lang, () -> reloaded = true,
                name -> name.equals("Sack") ? player : null, name -> name.equals("Lejano") ? offlinePlayer : null);
    }

    @Test
    void freeHiddenEmotesAreNotSuggestedNorShownButMagicStillPlaysThem() throws Exception {
        Files.writeString(folder.resolve("conjurar.yml"), "animation: conjurar\ncategory: magia\nhidden: true\n");
        catalog.load(List.of(folder.toFile()), w -> { throw new AssertionError(w); });
        EmotePlayer data = store.get(id);

        assertFalse(emote.suggest(player, "").contains("conjurar"));
        List<Emote> visible = catalog.visibleTo(e -> service.owns(player, data, e));
        assertEquals(List.of("dance", "wave"), visible.stream().map(Emote::id).toList());
        assertEquals(List.of("social"), EmoteCatalog.categories(visible));

        data.unlock("secret");
        assertTrue(catalog.visibleTo(e -> service.owns(player, data, e)).stream().anyMatch(e -> e.id().equals("secret")));
        assertEquals(LaunchResult.OK, service.forceMagic(player, "conjurar"));
    }

    @Test
    void suggestsOnlyOwnedVisibleEmotesPlusStopAndSlots() {
        store.get(id).toggleFavorite("wave");
        // dance bloqueado, secret oculto: no salen. 1 = favorito ocupado.
        assertEquals(List.of("1", "stop", "wave"), emote.suggest(player, ""));
        store.get(id).unlock("dance");
        store.get(id).unlock("secret");
        assertEquals(List.of("1", "dance", "secret", "stop", "wave"), emote.suggest(player, ""));
        assertEquals(List.of("stop"), emote.suggest(player, "st"));
    }

    @Test
    void adminSuggestsSubcommandsThenPlayersThenAllIds() {
        assertEquals(List.of("give", "list", "play", "reload", "take"), admin.suggest(new String[] { "" }));
        assertEquals(List.of("give"), admin.suggest(new String[] { "gi" }));
        assertEquals(List.of(), admin.suggest(new String[] { "reload", "" }));
        // Los ids salen todos, también los ocultos.
        assertEquals(List.of("dance", "secret", "wave"), admin.suggest(new String[] { "give", "Sack", "" }));
        assertEquals(List.of("wave"), admin.suggest(new String[] { "take", "Sack", "w" }));
    }

    @Test
    void giveFromConsoleUnlocksAndSaves() {
        admin.give(console, "Sack", "DANCE");
        assertTrue(store.get(id).unlocked().contains("dance"));
        verify(lang).send(eq(console), eq("admin.given"), any(Object[].class));
        // Guardado: un almacén nuevo lo lee del disco.
        EmotePlayerStore fresh = new EmotePlayerStore(new File(tmp.toFile(), "data"), 7);
        assertTrue(fresh.get(id).unlocked().contains("dance"));
    }

    @Test
    void takeLocks() {
        store.get(id).unlock("dance");
        admin.take(console, "Sack", "dance");
        assertFalse(store.get(id).unlocked().contains("dance"));
        verify(lang).send(eq(console), eq("admin.taken"), any(Object[].class));
    }

    @Test
    void playForcesForOnlinePlayer() {
        admin.play(console, "Sack", "dance"); // bloqueado: force lo ignora
        assertEquals(List.of("play:rpgroll_emotes/dance:false"), engine.calls);
    }

    @Test
    void unknownIdGivesMessage() {
        admin.give(console, "Sack", "nada");
        assertTrue(store.get(id).unlocked().isEmpty());
        verify(lang).send(eq(console), eq("admin.unknown_emote"), any(Object[].class));
    }

    @Test
    void offlineUnknownPlayerGivesMessage() {
        admin.give(console, "Fantasma", "wave");
        verify(lang).send(eq(console), eq("admin.player_not_found"), any(Object[].class));
    }

    @Test
    void giveToOfflineCachedPlayerSavesAndUnloads() {
        admin.give(console, "Lejano", "wave");
        EmotePlayerStore fresh = new EmotePlayerStore(new File(tmp.toFile(), "data"), 7);
        assertTrue(fresh.get(offlineId).unlocked().contains("wave"));
        verify(lang).send(eq(console), eq("admin.given"), any(Object[].class));
    }

    @Test
    void giveToProtectedPlayerReportsFailure() throws Exception {
        // Un directorio en vez del YAML: no se puede leer ni apartar, el jugador queda protegido.
        Files.createDirectories(tmp.resolve("data/data/players/" + id + ".yml"));
        admin.give(console, "Sack", "wave");
        verify(lang).send(eq(console), eq("admin.save_failed"), any(Object[].class));
        verify(lang, org.mockito.Mockito.never()).send(eq(console), eq("admin.given"), any(Object[].class));
        assertTrue(store.get(id).unlocked().isEmpty());
        admin.take(console, "Sack", "wave");
        verify(lang, org.mockito.Mockito.times(2)).send(eq(console), eq("admin.save_failed"), any(Object[].class));
    }

    @Test
    void playShowsCatalogId() {
        admin.play(console, "Sack", "WAVE");
        verify(lang).send(console, "admin.played", "player", "Sack", "id", "wave");
    }

    @Test
    void launchResultShowsOnActionBar() {
        emote.run(player, "dance"); // bloqueado
        verify(lang).component("launch.locked");
        assertTrue(engine.calls.isEmpty());
    }

    @Test
    void stopStopsCurrent() {
        emote.run(player, "wave");
        emote.run(player, "stop");
        assertEquals("stop", engine.calls.get(engine.calls.size() - 1));
    }

    @Test
    void emotesRapidoOpensQuickDialogAndIsSuggested() {
        List<Player> quick = new java.util.ArrayList<>();
        org.bukkit.command.CommandExecutor menu = emote.menuExecutor(quick::add);
        menu.onCommand(player, null, "emotes", new String[] { "rapido" });
        assertEquals(List.of(player), quick);
        assertEquals(List.of("rapido"), emote.menuCompleter().onTabComplete(player, null, "emotes", new String[] { "ra" }));
        assertEquals(List.of(), emote.menuCompleter().onTabComplete(player, null, "emotes", new String[] { "x", "" }));
    }

    @Test
    void reloadCallsCallback() {
        admin.reload(console);
        assertTrue(reloaded);
    }
}
