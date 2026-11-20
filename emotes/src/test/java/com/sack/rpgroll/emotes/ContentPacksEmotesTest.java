package com.sack.rpgroll.emotes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.catalog.EmoteParser;
import com.sack.rpgroll.emotes.install.AnimationInstaller;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los emotes de los packs temáticos ({@code packs/<pack>/RPGRoll-Emotes/}): se leen, su animación está en el
 * {@code .bbmodel} de su pack y alguna recompensa del pack los da con {@code emotesadmin give}.
 */
class ContentPacksEmotesTest {

    private static final Path PACKS = Path.of("../packs");
    private static final Set<String> EXPECTED = Set.of("pirata_hornpipe", "mareas_catalejo",
            "jade_saludo_marcial", "jade_postura_grulla");
    private static final Set<String> LOOPS = Set.of("pirata_hornpipe", "jade_postura_grulla");

    @Test
    void everyPackEmoteParsesAndItsAnimationExists() throws IOException {
        Set<String> seen = new HashSet<>();
        for (Path module : emoteModules()) {
            Path pack = module.getParent();
            List<Path> ymls;
            try (Stream<Path> s = Files.walk(module.resolve("emotes"))) {
                ymls = s.filter(p -> p.toString().endsWith(".yml")).toList();
            }
            assertFalse(ymls.isEmpty(), "sin emotes en " + module);
            for (Path yml : ymls) {
                String fileId = yml.getFileName().toString().replace(".yml", "");
                Emote e = EmoteParser.parse(fileId, YamlConfiguration.loadConfiguration(yml.toFile()),
                        "rpgroll_emotes");
                String where = pack.getFileName() + "/" + fileId;
                assertTrue(seen.add(e.id()), "id repetido: " + e.id());
                assertNotEquals("rpgroll_emotes", e.model(), where + ": el modelo de fábrica es reservado");

                File bbmodel = module.resolve("animations/" + e.model() + ".bbmodel").toFile();
                assertTrue(bbmodel.isFile(), where + ": falta " + bbmodel);
                assertNotEquals(AnimationInstaller.FACTORY_FILE, bbmodel.getName(), where);
                JsonObject root = JsonParser.parseString(Files.readString(bbmodel.toPath())).getAsJsonObject();
                // BetterModel registra el limb por el nombre del archivo; los dos deben coincidir.
                assertEquals(e.model(), root.get("model_identifier").getAsString(), where);
                JsonObject anim = null;
                for (JsonElement a : root.getAsJsonArray("animations")) {
                    if (a.getAsJsonObject().get("name").getAsString().equals(e.animation())) {
                        anim = a.getAsJsonObject();
                    }
                }
                assertNotNull(anim, where + ": la animación «" + e.animation() + "» no está en " + bbmodel);
                assertEquals(e.loop(), anim.get("loop").getAsString().equals("loop"), where + ": loop");
                assertEquals(LOOPS.contains(e.id()), e.loop(), where + ": bucle esperado");

                assertTrue(e.locked(), where + ": los de los packs se desbloquean con sus recompensas");
                assertTrue(e.howToUnlock() != null && !e.howToUnlock().isBlank(), where + ": falta how-to-unlock");
                assertTrue(givenByPack(pack, e.id()), where + ": ninguna recompensa del pack lo da");
            }
        }
        assertEquals(EXPECTED, seen);
    }

    private static List<Path> emoteModules() throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> packs = Files.list(PACKS)) {
            for (Path pack : packs.sorted().toList()) {
                Path module = pack.resolve("RPGRoll-Emotes");
                if (Files.isDirectory(module)) {
                    out.add(module);
                }
            }
        }
        return out;
    }

    /** Algún YAML del pack, fuera de RPGRoll-Emotes, lleva {@code emotesadmin give {player} <id>}. */
    private static boolean givenByPack(Path pack, String id) throws IOException {
        String command = "emotesadmin give {player} " + id;
        try (Stream<Path> s = Files.walk(pack)) {
            for (Path p : s.filter(p -> p.toString().endsWith(".yml"))
                    .filter(p -> !p.toString().contains("RPGRoll-Emotes")).toList()) {
                String text = Files.readString(p);
                if (text.contains(command + "\"") || text.contains(command + "\n") || text.contains(command + "\r")) {
                    return true;
                }
            }
        }
        return false;
    }
}
