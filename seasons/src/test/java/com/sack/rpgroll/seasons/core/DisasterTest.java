package com.sack.rpgroll.seasons.core;

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
                null, null, null, null);
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
        assertFalse(summerOnly.canRollIn(null, "world"));

        Disaster commandOnly = new Disaster("c", null, null, null, 200, 1, 0, List.of(), List.of(), List.of(),
                List.of(new DisasterPhenomenon(DisasterType.TORNADO, Map.of())), null, null, null, null);
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
        assertTrue(loaded.keySet().containsAll(List.of("tornado", "meteor_storm", "acid_rain", "earthquake")));

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
                    } else if (key.endsWith("blocks") || key.equals("crater-center") || key.equals("rock-fallback")) {
                        phenomenon.list(key, List.of()).forEach(name -> assertNotNull(Material.matchMaterial(name),
                                disaster.id() + " " + key + ": bloque desconocido " + name));
                    }
                }
            }
        }
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
