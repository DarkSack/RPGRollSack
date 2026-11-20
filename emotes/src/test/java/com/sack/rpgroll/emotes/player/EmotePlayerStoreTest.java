package com.sack.rpgroll.emotes.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmotePlayerStoreTest {

    @TempDir
    File dir;

    private final UUID id = UUID.randomUUID();

    @Test
    void roundTripsUnlockedAndFavorites() {
        EmotePlayerStore store = new EmotePlayerStore(dir, 7);
        EmotePlayer p = store.get(id);
        assertTrue(p.unlock("wave"));
        assertTrue(p.unlock("dance"));
        assertFalse(p.unlock("wave"));
        assertTrue(p.toggleFavorite("dance"));
        p.lastUseMillis(12345L);
        store.save(id);
        store.unload(id);

        EmotePlayer q = new EmotePlayerStore(dir, 7).get(id);
        assertEquals(2, q.unlocked().size());
        assertTrue(q.unlocked().contains("wave"));
        assertEquals("dance", q.favorite(1));
        assertNull(q.favorite(2));
        assertEquals(7, q.favorites().size());
        assertEquals(12345L, q.lastUseMillis());
    }

    @Test
    void favoritesCapAtMax() {
        EmotePlayer p = new EmotePlayerStore(dir, 7).get(id);
        for (int i = 1; i <= 7; i++) {
            assertTrue(p.toggleFavorite("e" + i));
        }
        assertFalse(p.toggleFavorite("e8"));
        assertEquals("e7", p.favorite(7));
    }

    @Test
    void toggleFavoriteRemovesAndFreesSlot() {
        EmotePlayer p = new EmotePlayerStore(dir, 7).get(id);
        p.toggleFavorite("a");
        p.toggleFavorite("b");
        assertTrue(p.toggleFavorite("a"));
        assertNull(p.favorite(1));
        assertEquals("b", p.favorite(2));
        assertTrue(p.toggleFavorite("c"));
        assertEquals("c", p.favorite(1));
    }

    @Test
    void missingFileIsEmptyPlayer() {
        EmotePlayer p = new EmotePlayerStore(dir, 7).get(id);
        assertTrue(p.unlocked().isEmpty());
        assertEquals(7, p.favorites().size());
        assertTrue(p.favorites().stream().allMatch(s -> s == null));
        assertEquals(0L, p.lastUseMillis());
    }

    @Test
    void unknownIdsSurviveRoundTrip() {
        EmotePlayerStore store = new EmotePlayerStore(dir, 7);
        store.get(id).unlock("emote_que_ya_no_existe");
        store.get(id).toggleFavorite("emote_que_ya_no_existe");
        store.saveAll();

        EmotePlayer q = new EmotePlayerStore(dir, 7).get(id);
        assertTrue(q.unlocked().contains("emote_que_ya_no_existe"));
        assertEquals("emote_que_ya_no_existe", q.favorite(1));
    }

    @Test
    void nonPositiveMaxFallsBackTo7() {
        assertEquals(7, new EmotePlayerStore(dir, 0).get(id).favorites().size());
        assertEquals(7, new EmotePlayerStore(dir, -3).get(UUID.randomUUID()).favorites().size());
    }

    @Test
    void corruptFileIsBackedUpNotOverwritten() throws Exception {
        File players = new File(dir, "data/players");
        players.mkdirs();
        File file = new File(players, id + ".yml");
        String garbage = "unlocked: [wave, {{{ : \n\t- ]]] basura";
        Files.writeString(file.toPath(), garbage);

        EmotePlayerStore store = new EmotePlayerStore(dir, 7);
        EmotePlayer p = store.get(id);
        assertTrue(p.unlocked().isEmpty());
        store.save(id);

        File[] backups = players.listFiles((d, n) -> n.startsWith(id + ".yml.corrupto-"));
        assertEquals(1, backups.length);
        assertEquals(garbage, Files.readString(backups[0].toPath()));
    }

    @Test
    void lockAlsoRemovesFavorite() {
        EmotePlayer p = new EmotePlayerStore(dir, 7).get(id);
        p.unlock("wave");
        p.toggleFavorite("wave");
        assertTrue(p.lock("wave"));
        assertFalse(p.unlocked().contains("wave"));
        assertNull(p.favorite(1));
        assertFalse(p.lock("wave"));
    }

    @Test
    void idsAreLowercased() {
        EmotePlayer p = new EmotePlayerStore(dir, 7).get(id);
        assertTrue(p.unlock("Wave"));
        assertTrue(p.unlocked().contains("wave"));
        assertFalse(p.unlock("WAVE"));
        p.toggleFavorite("Dance");
        assertEquals("dance", p.favorite(1));
    }

    @Test
    void extraFavoritesBeyondMaxAreDropped() {
        EmotePlayerStore big = new EmotePlayerStore(dir, 7);
        EmotePlayer p = big.get(id);
        for (int i = 1; i <= 5; i++) {
            p.toggleFavorite("e" + i);
        }
        big.saveAll();

        EmotePlayer q = new EmotePlayerStore(dir, 3).get(id);
        assertEquals(3, q.favorites().size());
        assertEquals("e3", q.favorite(3));
        assertNull(q.favorite(4));
    }

    @Test
    void blankIdIsIgnored() {
        EmotePlayer p = new EmotePlayerStore(dir, 7).get(id);
        assertFalse(p.unlock(""));
        assertFalse(p.unlock(null));
        assertFalse(p.unlock("  "));
        assertFalse(p.toggleFavorite(null));
        assertTrue(p.unlocked().isEmpty());
        assertNull(p.favorite(1));
    }

    @Test
    void readErrorDoesNotRenameOrOverwrite() {
        // Una carpeta con el nombre del YAML hace fallar la lectura con IOException, sin corrupción.
        File players = new File(dir, "data/players");
        File file = new File(players, id + ".yml");
        assertTrue(file.mkdirs());

        EmotePlayerStore store = new EmotePlayerStore(dir, 7);
        store.get(id).unlock("wave");
        store.save(id);
        store.saveAll();
        store.unload(id);

        assertTrue(file.isDirectory());
        assertEquals(0, players.listFiles((d, n) -> n.contains("corrupto")).length);
    }

    @Test
    void failedBackupIsNeverOverwrittenBySave() throws Exception {
        File players = new File(dir, "data/players");
        players.mkdirs();
        File file = new File(players, id + ".yml");
        String garbage = "unlocked: [wave, {{{ : \n\t- ]]] basura";
        Files.writeString(file.toPath(), garbage);

        EmotePlayerStore store = new EmotePlayerStore(dir, 7) {
            @Override
            boolean rename(File from, File to) {
                return false;
            }
        };
        store.get(id).unlock("wave");
        store.save(id);
        store.saveAll();
        store.unload(id);

        assertEquals(garbage, Files.readString(file.toPath()));

        // Tras descargar, la marca se quita: con un store normal el archivo se aparta y se puede guardar.
        EmotePlayerStore normal = new EmotePlayerStore(dir, 7);
        normal.get(id).unlock("wave");
        normal.save(id);
        assertTrue(Files.readString(file.toPath()).contains("wave"));
    }

    @Test
    void lockIgnoresBlankId() {
        EmotePlayer p = new EmotePlayerStore(dir, 7).get(id);
        p.unlock("wave");
        assertFalse(p.lock(""));
        assertFalse(p.lock(null));
        assertTrue(p.unlocked().contains("wave"));
    }

    @Test
    void outOfRangeFavoriteIsNull() {
        EmotePlayer p = new EmotePlayerStore(dir, 7).get(id);
        p.toggleFavorite("a");
        assertNull(p.favorite(0));
        assertNull(p.favorite(-1));
        assertNull(p.favorite(8));
    }
}
