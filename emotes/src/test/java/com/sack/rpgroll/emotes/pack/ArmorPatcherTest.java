package com.sack.rpgroll.emotes.pack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Armaduras propias (equipment de otros módulos de SRP) en el modelo del jugador de BetterModel. */
class ArmorPatcherTest {

    @TempDir
    Path content;

    private Path bm;
    private final List<String> log = new ArrayList<>();

    @BeforeEach
    void pack() throws IOException {
        bm = content.resolve("bettermodel/assets/bettermodel");
        // Lo que genera BetterModel: select por tipo de armadura (custom_model_data índice 0) con netherite.
        write(bm.resolve("items/b/g.json"), """
                {"model": {"type": "composite", "models": [{"type": "composite", "models": [
                  {"type": "select", "property": "custom_model_data", "index": 0, "fallback": {"type": "empty"},
                   "cases": [
                     {"when": "diamond", "model": {"type": "model", "model": "bettermodel:a/1"}},
                     {"when": "netherite", "model": {"type": "model", "model": "bettermodel:a/5b",
                       "tints": [{"type": "custom_model_data", "index": 0, "default": 16777215}]}}]},
                  {"type": "select", "property": "custom_model_data", "index": 1, "fallback": {"type": "empty"},
                   "cases": [{"when": "bolt", "model": {"type": "model", "model": "bettermodel:a/7c"}}]}]}]}}
                """);
        write(bm.resolve("models/a/5b.json"), """
                {"textures": {"0": "bettermodel:item/a/n", "1": "bettermodel:item/a/l",
                  "particle": "bettermodel:item/a/n"}, "elements": []}
                """);
        png(bm.resolve("textures/item/a/n.png"), true);   // capa de cuerpo: casco opaco
        png(bm.resolve("textures/item/a/l.png"), false);  // capa de piernas: cabeza vacía
        // Una armadura propia en otro módulo.
        Path items = content.resolve("sackito-items/assets/sackito");
        write(items.resolve("equipment/vacuita.json"), """
                {"layers": {"humanoid": [{"texture": "sackito:vacuita"}],
                  "humanoid_leggings": [{"texture": "sackito:vacuita"}]}}
                """);
        png(items.resolve("textures/entity/equipment/humanoid/vacuita.png"), true);
        png(items.resolve("textures/entity/equipment/humanoid_leggings/vacuita.png"), false);
    }

    @Test
    void addsACaseForEachCustomArmorBasedOnNetherite() throws IOException {
        int patched = new ArmorPatcher(content, log::add).patch();

        assertEquals(1, patched);
        JsonObject select = armorSelect();
        JsonObject vacuita = caseFor(select, "vacuita");
        JsonObject model = vacuita.getAsJsonObject("model");
        assertEquals("bettermodel:rpgroll_armor/vacuita/a_5b", model.get("model").getAsString());
        // Los tintes y demás campos se copian del caso de netherite.
        assertEquals(1, model.getAsJsonArray("tints").size());

        JsonObject copy = read(bm.resolve("models/rpgroll_armor/vacuita/a_5b.json"));
        JsonObject textures = copy.getAsJsonObject("textures");
        assertEquals("bettermodel:item/rpgroll_armor/vacuita", textures.get("0").getAsString());
        assertEquals("bettermodel:item/rpgroll_armor/vacuita_leggings", textures.get("1").getAsString());
        assertEquals("bettermodel:item/rpgroll_armor/vacuita", textures.get("particle").getAsString());
        assertArrayEquals(Files.readAllBytes(content.resolve(
                        "sackito-items/assets/sackito/textures/entity/equipment/humanoid/vacuita.png")),
                Files.readAllBytes(bm.resolve("textures/item/rpgroll_armor/vacuita.png")));
        assertTrue(Files.isRegularFile(bm.resolve("textures/item/rpgroll_armor/vacuita_leggings.png")));
    }

    @Test
    void runningTwiceKeepsOneCase() throws IOException {
        new ArmorPatcher(content, log::add).patch();
        new ArmorPatcher(content, log::add).patch();

        long count = 0;
        for (var c : armorSelect().getAsJsonArray("cases")) {
            if (c.getAsJsonObject().get("when").getAsString().equals("vacuita")) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    void leavesTrimsAndVanillaTypesAlone() throws IOException {
        // Un equipment propio que se llama como uno de vanilla no pisa el caso de vanilla.
        write(content.resolve("sackito-items/assets/sackito/equipment/diamond.json"),
                "{\"layers\": {\"humanoid\": [{\"texture\": \"sackito:vacuita\"}]}}");

        new ArmorPatcher(content, log::add).patch();

        JsonObject select = armorSelect();
        assertEquals("bettermodel:a/1", caseFor(select, "diamond").getAsJsonObject("model").get("model").getAsString());
        JsonArray trims = read(bm.resolve("items/b/g.json")).getAsJsonObject("model").getAsJsonArray("models")
                .get(0).getAsJsonObject().getAsJsonArray("models").get(1).getAsJsonObject().getAsJsonArray("cases");
        assertEquals(1, trims.size());
    }

    @Test
    void aMissingLeggingsLayerBecomesTransparent() throws IOException {
        Files.delete(content.resolve("sackito-items/assets/sackito/textures/entity/equipment/humanoid_leggings/vacuita.png"));
        write(content.resolve("sackito-items/assets/sackito/equipment/vacuita.json"),
                "{\"layers\": {\"humanoid\": [{\"texture\": \"sackito:vacuita\"}]}}");

        new ArmorPatcher(content, log::add).patch();

        BufferedImage legs = ImageIO.read(bm.resolve("textures/item/rpgroll_armor/vacuita_leggings.png").toFile());
        assertEquals(64, legs.getWidth());
        assertEquals(0, legs.getRGB(40, 20) >>> 24, "sin capa de piernas, la textura queda transparente");
    }

    @Test
    void withoutBetterModelPackItDoesNothing() throws IOException {
        Path empty = content.resolve("otro");
        Files.createDirectories(empty);
        assertEquals(0, new ArmorPatcher(empty, log::add).patch());
    }

    private JsonObject armorSelect() throws IOException {
        return read(bm.resolve("items/b/g.json")).getAsJsonObject("model").getAsJsonArray("models").get(0)
                .getAsJsonObject().getAsJsonArray("models").get(0).getAsJsonObject();
    }

    private static JsonObject caseFor(JsonObject select, String when) {
        for (var c : select.getAsJsonArray("cases")) {
            if (c.getAsJsonObject().get("when").getAsString().equals(when)) {
                return c.getAsJsonObject();
            }
        }
        throw new AssertionError("no hay caso " + when);
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static void write(Path file, String json) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    /** 64×32 con la zona de la cabeza (0-32, 0-16) opaca o no, y el resto opaco. */
    private static void png(Path file, boolean head) throws IOException {
        BufferedImage im = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                boolean inHead = x < 32 && y < 16;
                im.setRGB(x, y, inHead && !head ? 0 : 0xFF8040C0);
            }
        }
        Files.createDirectories(file.getParent());
        ImageIO.write(im, "png", file.toFile());
    }
}
