package com.sack.rpgroll.disasters.core;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisasterTest {

    private static Disaster disaster(String id, List<String> combine, DisasterType... types) {
        List<DisasterPhenomenon> phenomena = java.util.Arrays.stream(types)
                .map(type -> new DisasterPhenomenon(type, Map.of())).toList();
        return new Disaster(id, id, null, null, 200, 1, 0.5, List.of("summer"), List.of(), combine, phenomena,
                null, null, null, null, null, -1, null, null);
    }

    @Test
    void regenerationOfTheDisasterAppliesUnlessThePhenomenonSetsItsOwn() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("id", "r");
        config.set("regenerate", false);
        config.set("regenerate-seconds", 600);
        config.set("phenomena", List.of(Map.of("type", "TORNADO"), Map.of("type", "EARTHQUAKE", "regenerate", true)));
        Disaster parsed = assertDoesNotThrow(() -> new DisasterParser().parse(config));

        assertEquals(Map.of("regenerate", false, "regenerate-seconds", 600), parsed.regeneration());
        DisasterPhenomenon tornado = parsed.phenomena().get(0).withDefaults(parsed.regeneration());
        DisasterPhenomenon quake = parsed.phenomena().get(1).withDefaults(parsed.regeneration());
        assertFalse(tornado.flag("regenerate", true));
        assertTrue(quake.flag("regenerate", false));
        assertEquals(600, quake.number("regenerate-seconds", 300));
        assertEquals(60, quake.number("regenerate-spread-seconds", 60));
    }

    @Test
    void combineAddsOtherDisastersOnceAndSurvivesCycles() {
        Map<String, Disaster> all = new HashMap<>();
        all.put("a", disaster("a", List.of("b", "c"), DisasterType.TORNADO));
        all.put("b", disaster("b", List.of("a"), DisasterType.EARTHQUAKE));
        all.put("c", disaster("c", List.of("b", "missing"), DisasterType.ACID_RAIN));

        List<DisasterType> types = all.get("a").allPhenomena(id -> Optional.ofNullable(all.get(id))).stream()
                .map(DisasterPhenomenon::type).toList();

        assertEquals(List.of(DisasterType.TORNADO, DisasterType.EARTHQUAKE, DisasterType.ACID_RAIN), types);
    }

    @Test
    void dailyRollRespectsChanceSeasonAndWorld() {
        Disaster summerOnly = disaster("s", List.of(), DisasterType.TORNADO);
        assertTrue(summerOnly.canRollIn("summer", "world"));
        assertFalse(summerOnly.canRollIn("winter", "world"));
        // Sin RPGRoll-Seasons (o en un mundo sin estaciones) no se filtra por estación.
        assertTrue(summerOnly.canRollIn(null, "world"));

        Disaster commandOnly = new Disaster("c", null, null, null, 200, 1, 0, List.of(), List.of(), List.of(),
                List.of(new DisasterPhenomenon(DisasterType.TORNADO, Map.of())), null, null, null, null, null, -1, null,
                null);
        assertFalse(commandOnly.canRollIn("summer", "world"));
    }

    @Test
    void listParamsAcceptYamlListsCommaTextAndExplicitEmpty() {
        DisasterPhenomenon phenomenon = new DisasterPhenomenon(DisasterType.TORNADO,
                Map.of("a", List.of("DIRT", "SAND"), "b", "DIRT, SAND", "c", List.of()));
        assertEquals(List.of("DIRT", "SAND"), phenomenon.list("a", List.of("X")));
        assertEquals(List.of("DIRT", "SAND"), phenomenon.list("b", List.of("X")));
        assertEquals(List.of(), phenomenon.list("c", List.of("X")));
        assertEquals(List.of("X"), phenomenon.list("missing", List.of("X")));
    }

    @Test
    void levelsAreParsedAndPickedByWeight() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("levels.1.weight", 3);
        config.set("levels.2.weight", 1);
        config.set("levels.2.intensity", 2.5);
        config.set("levels.3.weight", 0);
        Map<Integer, DisasterLevel> levels = DisasterLevel.parse(config.getConfigurationSection("levels"));

        assertEquals(2.5, levels.get(2).intensity());
        assertEquals(1.0, levels.get(1).size());
        assertEquals(1, DisasterLevel.pick(levels, 0.0));
        assertEquals(1, DisasterLevel.pick(levels, 0.74));
        assertEquals(2, DisasterLevel.pick(levels, 0.75));
        assertEquals(2, DisasterLevel.pick(levels, 0.999));
        assertEquals(null, DisasterLevel.pick(Map.of(1, new DisasterLevel(0, 1, 1, 1)), 0.5));

        YamlConfiguration bad = new YamlConfiguration();
        bad.set("levels.dos.weight", 1);
        assertThrows(IllegalArgumentException.class, () -> DisasterLevel.parse(bad.getConfigurationSection("levels")));
    }

    @Test
    void parserRejectsUnknownPhenomenonAndEmptyDisaster() {
        YamlConfiguration bad = new YamlConfiguration();
        bad.set("id", "bad");
        bad.set("phenomena", List.of(Map.of("type", "VOLCANO")));
        assertThrows(IllegalArgumentException.class, () -> new DisasterParser().parse(bad));

        YamlConfiguration empty = new YamlConfiguration();
        empty.set("id", "empty");
        assertThrows(IllegalArgumentException.class, () -> new DisasterParser().parse(empty));
    }

    /** Los desastres que vienen con el plugin cargan, sus combine existen y sus partículas y bloques son válidos. */
    @Test
    void bundledDisastersAreValid() throws Exception {

        File[] files = new File("src/main/resources/disasters").listFiles((dir, name) -> name.endsWith(".yml"));
        assertNotNull(files);

        Map<String, Disaster> loaded = new HashMap<>();
        for (File file : files) {
            Disaster disaster = new DisasterParser().parse(YamlConfiguration.loadConfiguration(file));
            loaded.put(disaster.id(), disaster);
        }
        assertTrue(loaded.keySet().containsAll(List.of("tornado", "meteor_storm", "acid_rain", "earthquake", "sinkhole", "tsunami", "sandstorm", "doomsday")));

        for (Disaster disaster : loaded.values()) {
            for (String other : disaster.combine()) {
                assertTrue(loaded.containsKey(other), disaster.id() + " combina un desastre que no existe: " + other);
            }
            checkSound(disaster.id() + " start-sound", disaster.startSound());
            for (DisasterPhenomenon phenomenon : disaster.phenomena()) {
                for (var entry : phenomenon.params().entrySet()) {
                    String key = entry.getKey();
                    if (key.endsWith("sound")) {
                        checkSound(disaster.id() + " " + key, String.valueOf(entry.getValue()));
                    } else if (key.endsWith("particles") && !key.equals("ground-particles")) {
                        phenomenon.list(key, List.of()).forEach(spec -> checkParticle(disaster.id(), spec));
                    } else if (key.endsWith("blocks") && !(entry.getValue() instanceof Boolean)
                            || key.equals("rock-fallback") || key.equals("cover")) {
                        phenomenon.list(key, List.of()).forEach(name -> checkBlock(disaster.id() + " " + key, name));
                    } else if (key.equals("corrode")) {
                        for (String rule : phenomenon.list(key, List.of())) {
                            String[] parts = rule.split(">");
                            assertEquals(2, parts.length, disaster.id() + " corrode: falta '>' en " + rule);
                            checkBlock(disaster.id() + " corrode", parts[0]);
                            checkBlock(disaster.id() + " corrode", parts[1]);
                        }
                    } else if (key.equals("entities")) {
                        for (String mob : phenomenon.list(key, List.of())) {
                            String name = mob.replaceAll("\\*\\d+$", "");
                            if (!name.startsWith("rpgroll:")) {
                                assertTrue(org.bukkit.entity.EntityType.valueOf(name).isAlive(),
                                        disaster.id() + " entities: " + mob + " no es un ser vivo");
                            }
                        }
                    }
                }
            }
        }
    }

    /** Un bloque, con o sin peso ({@code X*3}) o comodín ({@code *_LEAVES}). */
    private static void checkBlock(String where, String spec) {
        String name = spec.replaceAll("\\*\\d+$", "");
        assertTrue(name.contains("*")
                        ? java.util.Arrays.stream(Material.values()).anyMatch(m -> m.name().matches(name.replace("*", ".*")))
                        : Material.matchMaterial(name) != null,
                where + ": bloque desconocido " + spec);
    }

    /** Un nombre de la lista de Bukkit o una clave {@code espacio:ruta}, y luego volumen y tono como números. */
    private static void checkSound(String where, String spec) {
        String[] parts = spec.trim().split("\\s+");
        if (parts[0].isEmpty()) {
            return;
        }
        assertTrue(parts[0].matches("[A-Z0-9_]+") || org.bukkit.NamespacedKey.fromString(parts[0]) != null,
                where + ": sonido mal escrito " + spec);
        for (int i = 1; i < parts.length; i++) {
            Float.parseFloat(parts[i]);
        }
    }

    private static void checkParticle(String disasterId, String spec) {
        String[] parts = spec.trim().split("\\s+");
        String name = parts[0].split("\\*")[0].toUpperCase(Locale.ROOT);
        Particle particle = Particle.valueOf(name);
        if (particle.getDataType() == org.bukkit.block.data.BlockData.class && parts.length > 1) {
            assertNotNull(Material.matchMaterial(parts[1]), disasterId + ": bloque de partícula desconocido " + parts[1]);
        }
    }

}
