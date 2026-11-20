package com.sack.rpgroll.emotes.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** El contenido de fábrica: los {@code .yml} de {@code emotes/} y las animaciones de {@code animations/}. */
class BundledCatalogTest {

    private static final File RESOURCES = new File("src/main/resources");
    private static final Set<String> LOCKED = Set.of("robot", "bailar_lento", "flexionar", "meditar");
    private static final Set<String> LOOPS = Set.of("saltitos", "bailar_lento", "sentarse", "meditar", "canalizar");
    private static final Set<String> CATEGORIES = Set.of("social", "baile", "accion", "magia");

    private static EmoteCatalog catalog;
    private static final List<String> warns = new ArrayList<>();
    /** modelo (nombre del archivo) → animación → objeto JSON de la animación. */
    private static final Map<String, Map<String, JsonObject>> models = new HashMap<>();

    @BeforeAll
    static void load() throws Exception {
        catalog = new EmoteCatalog("rpgroll_emotes");
        catalog.load(List.of(new File(RESOURCES, "emotes")), warns::add);
        File[] files = new File(RESOURCES, "animations").listFiles((d, n) -> n.endsWith(".bbmodel"));
        assertNotNull(files);
        for (File f : files) {
            JsonObject root = JsonParser.parseString(Files.readString(f.toPath())).getAsJsonObject();
            Map<String, JsonObject> anims = new HashMap<>();
            for (JsonElement a : root.getAsJsonArray("animations")) {
                anims.put(a.getAsJsonObject().get("name").getAsString(), a.getAsJsonObject());
            }
            String key = f.getName().substring(0, f.getName().length() - ".bbmodel".length());
            models.put(key, anims);
        }
    }

    @Test
    void loadsTwentyThreeWithoutWarnings() {
        assertEquals(List.of(), warns);
        assertEquals(23, catalog.all().size());
        assertTrue(catalog.get("conjurar_caminando").isPresent());
        assertTrue(catalog.get("canalizar").isPresent());
    }

    @Test
    void everyAnimationExistsAndLoopMatches() {
        for (Emote e : catalog.all()) {
            JsonObject anim = models.getOrDefault(e.model(), Map.of()).get(e.animation());
            assertNotNull(anim, "falta la animación «" + e.animation() + "» del emote " + e.id());
            // «hold» (sentarse) también dura hasta que se corta: baja una vez y se queda en la última pose.
            String mode = anim.get("loop").getAsString();
            assertEquals(e.loop(), mode.equals("loop") || mode.equals("hold"), "loop de " + e.id());
            assertEquals(LOOPS.contains(e.id()), e.loop(), "bucle esperado en " + e.id());
        }
    }

    @Test
    void locksCategoriesAndHiddenMatchTheSpec() {
        for (Emote e : catalog.all()) {
            assertEquals(LOCKED.contains(e.id()), e.locked(), "locked de " + e.id());
            assertTrue(CATEGORIES.contains(e.category()), "categoría de " + e.id());
            assertEquals(e.category().equals("magia"), e.hidden(), "hidden de " + e.id());
            assertEquals(null, e.music(), "los de fábrica salen sin música: " + e.id());
        }
        assertEquals("magia", catalog.get("conjurar").orElseThrow().category());
        assertEquals("magia", catalog.get("canalizar").orElseThrow().category());
    }

    @Test
    void durationsFollowTheRules() {
        for (Emote e : catalog.all()) {
            float length = models.get(e.model()).get(e.animation()).get("length").getAsFloat();
            if (e.id().startsWith("conjurar")) {
                assertEquals(0.8f, length, 1e-4, e.id());
            } else if (!e.loop() && !e.id().equals("bailar")) {
                // bailar (4 s) es el del prototipo probado en el juego y se conserva tal cual.
                assertTrue(length >= 1.5f && length <= 3f, e.id() + " dura " + length);
            }
        }
    }

    @Test
    void raritiesAreTranslated() {
        for (String lang : List.of("es", "en", "pt_BR")) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(RESOURCES, "lang/" + lang + ".yml"));
            for (String rarity : EmoteCatalog.RARITY_ORDER) {
                assertTrue(yml.isString("rarity." + rarity), "falta rarity." + rarity + " en " + lang);
            }
        }
    }

    @Test
    void namesSoundsAndParticlesAreUsable() throws Exception {
        for (String lang : List.of("es", "en", "pt_BR")) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(RESOURCES, "lang/" + lang + ".yml"));
            for (Emote e : catalog.all()) {
                assertTrue(e.name().startsWith("lang:"), e.id());
                assertTrue(yml.isString(e.name().substring(5)), "falta " + e.name() + " en " + lang);
            }
        }
        for (Emote e : catalog.all()) {
            assertNotNull(Material.matchMaterial(e.icon()), e.id() + ": icono " + e.icon());
            if (e.sound() != null) {
                // World#playSound(Location, String, ...) espera una clave de sonido, no el nombre del enum.
                assertTrue(Key.parseable(e.sound().id()), e.id() + ": " + e.sound().id());
            }
            if (e.particles() != null) {
                // EmoteService las lanza sin datos: solo valen las partículas que no los piden.
                assertEquals(Void.class, Particle.valueOf(e.particles().type()).getDataType(), e.id());
            }
        }
    }
}
