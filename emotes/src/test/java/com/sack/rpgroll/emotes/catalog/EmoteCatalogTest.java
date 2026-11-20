package com.sack.rpgroll.emotes.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

class EmoteCatalogTest {

    @TempDir
    Path tmp;

    private static YamlConfiguration yaml(String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return y;
    }

    private File write(String dir, String name, String text) throws Exception {
        Path folder = tmp.resolve(dir);
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(name), text);
        return folder.toFile();
    }

    @Test
    void parsesFullEmote() {
        Emote e = EmoteParser.parse("saludar_archivo", yaml("""
                id: Saludar
                name: "<gold>Saludar"
                icon: PLAYER_HEAD
                category: Social
                animation: saludar
                model: rpgroll_emotes
                loop: true
                unlock: locked
                rarity: Rare
                sound: { id: ENTITY_PLAYER_LEVELUP, volume: 0.6, pitch: 1.4 }
                particles: { type: NOTE, count: 6 }
                cancel: [move, damage]
                hidden: true
                how-to-unlock: "Pase nivel 20"
                """), "por_defecto");

        assertEquals("saludar", e.id());
        assertEquals("<gold>Saludar", e.name());
        assertEquals("PLAYER_HEAD", e.icon());
        assertEquals("social", e.category());
        assertEquals("saludar", e.animation());
        assertEquals("rpgroll_emotes", e.model());
        assertTrue(e.loop());
        assertTrue(e.locked());
        assertEquals("rare", e.rarity());
        assertEquals("ENTITY_PLAYER_LEVELUP", e.sound().id());
        assertEquals(0.6f, e.sound().volume());
        assertEquals(1.4f, e.sound().pitch());
        assertEquals("NOTE", e.particles().type());
        assertEquals(6, e.particles().count());
        assertEquals(EnumSet.of(CancelReason.MOVE, CancelReason.DAMAGE), e.cancel());
        assertTrue(e.hidden());
        assertEquals("Pase nivel 20", e.howToUnlock());
    }

    @Test
    void parsesMusic() {
        Emote e = EmoteParser.parse("x", yaml("""
                animation: a
                music: { sound: "RPGRoll_Emotes:Musica.Baile1", volume: 0.5, length: 12.5 }
                """), "m");
        assertEquals("rpgroll_emotes:musica.baile1", e.music().sound());
        assertEquals(0.5f, e.music().volume());
        assertEquals(12.5, e.music().lengthSeconds());
    }

    @Test
    void musicWithoutSoundIsIgnoredWithWarning() {
        List<String> warnings = new ArrayList<>();
        Emote e = EmoteParser.parse("x", yaml("animation: a\nmusic: { volume: 2 }"), "m", warnings::add);
        assertNull(e.music());
        assertEquals(1, warnings.size());
    }

    @Test
    void invalidMusicKeyIsIgnoredWithWarning() {
        List<String> warnings = new ArrayList<>();
        Emote e = EmoteParser.parse("arch", yaml("animation: a\nmusic: { sound: \"Mi Sonido\" }"), "m", warnings::add);
        assertNull(e.music());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("arch"));
    }

    @Test
    void musicWarningReachesCatalogWarn() throws Exception {
        File f = write("musica", "baile.yml", "animation: a\nmusic: { volume: 2 }\n");
        List<String> warns = new ArrayList<>();
        new EmoteCatalog().load(List.of(f), warns::add);
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains("baile"));
    }

    @Test
    void musicDefaults() {
        Emote e = EmoteParser.parse("x", yaml("animation: a\nmusic: { sound: s }"), "m");
        assertEquals(1f, e.music().volume());
        assertEquals(0.0, e.music().lengthSeconds());
        assertNull(EmoteParser.parse("x", yaml("animation: a"), "m").music());
    }

    @Test
    void volumeIsClamped() {
        assertEquals(4f, EmoteParser.parse("x", yaml("animation: a\nmusic: { sound: s, volume: 9 }"), "m")
                .music().volume());
        assertEquals(0f, EmoteParser.parse("x", yaml("animation: a\nmusic: { sound: s, volume: -1 }"), "m")
                .music().volume());
    }

    @Test
    void defaultsCancelToAllAndIdToFileName() {
        Emote e = EmoteParser.parse("Aplaudir", yaml("animation: aplaudir"), "rpgroll_emotes");

        assertEquals("aplaudir", e.id());
        assertEquals("rpgroll_emotes", e.model());
        assertEquals(EnumSet.allOf(CancelReason.class), e.cancel());
        assertFalse(e.locked());
        assertFalse(e.loop());
        assertFalse(e.hidden());
        assertNull(e.sound());
        assertNull(e.particles());
        assertNull(e.howToUnlock());
    }

    @Test
    void cancelAsSingleStringIsAccepted() {
        Emote e = EmoteParser.parse("x", yaml("animation: a\ncancel: move"), "m");
        assertEquals(EnumSet.of(CancelReason.MOVE), e.cancel());
        assertThrows(IllegalArgumentException.class,
                () -> EmoteParser.parse("x", yaml("animation: a\ncancel: volar"), "m"));
    }

    @Test
    void emptyCancelListMeansNeverCancels() {
        Emote e = EmoteParser.parse("x", yaml("animation: a\ncancel: []"), "m");
        assertTrue(e.cancel().isEmpty());
    }

    @Test
    void cancelSetIsImmutable() {
        Emote e = EmoteParser.parse("x", yaml("animation: a"), "m");
        assertThrows(UnsupportedOperationException.class, () -> e.cancel().remove(CancelReason.MOVE));
    }

    @Test
    void rejectsMissingAnimation() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EmoteParser.parse("x", yaml("name: X"), "m"));
        assertTrue(ex.getMessage().contains("animation"));
    }

    @Test
    void rejectsUnknownUnlock() {
        assertThrows(IllegalArgumentException.class,
                () -> EmoteParser.parse("x", yaml("animation: a\nunlock: quizas"), "m"));
        assertThrows(IllegalArgumentException.class,
                () -> EmoteParser.parse("x", yaml("animation: a\ncancel: [move, volar]"), "m"));
    }

    @Test
    void duplicateIdLastWinsWithWarning() throws Exception {
        File a = write("a", "dup.yml", "animation: uno");
        File b = write("b", "dup.yml", "animation: dos");
        List<String> warns = new ArrayList<>();
        EmoteCatalog catalog = new EmoteCatalog();

        catalog.load(List.of(a, b), warns::add);

        assertEquals("dos", catalog.get("dup").orElseThrow().animation());
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains("dup"));
    }

    @Test
    void missingAnimationIsDroppedWithWarning() throws Exception {
        File f = write("c", "bueno.yml", "animation: si");
        write("c/sub", "malo.yml", "animation: no");
        List<String> warns = new ArrayList<>();
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(f), warns::add);

        catalog.checkAnimations((model, anim) -> anim.equals("si"), warns::add);

        assertTrue(catalog.get("bueno").isPresent());
        assertTrue(catalog.get("malo").isEmpty());
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains("malo"));
    }

    @Test
    void hiddenOnlyVisibleWhenLockedAndOwned() throws Exception {
        File f = write("d", "visible.yml", "animation: a");
        write("d", "secreto.yml", "animation: b\nunlock: locked\nhidden: true");
        // Libre y oculto (como conjurar): no sale nunca; lo lanza Magic.
        write("d", "magia.yml", "animation: c\nhidden: true");
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(f), w -> { });

        assertEquals(List.of("visible"), catalog.visibleTo(e -> false).stream().map(Emote::id).toList());
        assertEquals(List.of("secreto", "visible"), catalog.visibleTo(e -> true).stream().map(Emote::id).toList());
    }

    @Test
    void categoriesOfVisibleSkipCategoryWithOnlyHiddenEmotes() throws Exception {
        File f = write("h", "saludar.yml", "animation: a\ncategory: social");
        write("h", "conjurar.yml", "animation: b\ncategory: magia\nhidden: true");
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(f), w -> { });

        assertEquals(List.of("social"), EmoteCatalog.categories(catalog.visibleTo(e -> true)));
    }

    @Test
    void unknownUnlockedIdsAreKeptButHidden() throws Exception {
        File f = write("e", "uno.yml", "animation: a");
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(f), w -> { });
        Set<String> owned = Set.of("uno", "ya_no_existe");

        List<Emote> visible = catalog.visibleTo(e -> owned.contains(e.id()));

        assertEquals(1, visible.size());
        assertTrue(catalog.get("ya_no_existe").isEmpty());
    }

    @Test
    void allIsSortedByCategoryRarityId() throws Exception {
        // El nombre es una clave de lang («lang:emotes.<id>»): ordenar por él no tiene sentido, se ordena por id.
        File f = write("g", "z.yml", "animation: a\ncategory: baile\nrarity: rare\nname: B");
        write("g", "y.yml", "animation: a\ncategory: baile\nrarity: common\nname: A");
        write("g", "x.yml", "animation: a\ncategory: accion\nrarity: epic\nname: A");
        write("g", "w.yml", "animation: a\ncategory: baile\nrarity: common\nname: Z");
        EmoteCatalog catalog = new EmoteCatalog();
        catalog.load(List.of(f), w -> { });

        assertEquals(List.of("x", "w", "y", "z"), catalog.all().stream().map(Emote::id).toList());
        assertEquals(List.of("accion", "baile"), EmoteCatalog.categories(catalog.all()));
    }
}
