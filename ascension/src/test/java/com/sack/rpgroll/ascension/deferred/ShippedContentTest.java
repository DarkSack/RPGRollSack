package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.progress.TriggerType;
import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * El contenido que trae el plugin se parsea, y las referencias entre
 * ficheros (títulos que dan los logros, facciones rivales…) existen. Los
 * {@code _reference_full.yml} también se parsean: son documentación, pero
 * quien copie uno tiene que obtener un fichero válido.
 */
class ShippedContentTest {

    private static final File RESOURCES = new File("src/main/resources");

    private static <T extends RPGContent> List<T> parseAll(String folder, ContentParser<T> parser, boolean includeReference) {

        File[] files = new File(RESOURCES, folder).listFiles((dir, name) -> name.endsWith(".yml"));
        List<T> parsed = new ArrayList<>();

        if (files == null) {
            fail("no existe la carpeta " + folder);
        }

        for (File file : files) {
            if (!includeReference && file.getName().startsWith("_")) {
                continue;
            }
            try {
                parsed.add(parser.parse(YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                fail(folder + "/" + file.getName() + ": " + e.getMessage());
            }
        }

        return parsed;
    }

    @Test
    void everyShippedFileParses() {
        assertFalse(parseAll("achievements", new AchievementParser(), true).isEmpty());
        assertFalse(parseAll("factions", new FactionParser(), true).isEmpty());
        assertFalse(parseAll("titles", new TitleParser(), true).isEmpty());
        assertFalse(parseAll("job-evolutions", new JobEvolutionParser(), true).isEmpty());
        assertFalse(parseAll("secrets", new SecretUnlockParser(), true).isEmpty());
    }

    @Test
    void referencedTitlesAndFactionsExist() {

        Set<String> titles = parseAll("titles", new TitleParser(), false).stream()
                .map(Title::id).collect(Collectors.toSet());
        List<Faction> factions = parseAll("factions", new FactionParser(), false);
        Set<String> factionIds = factions.stream().map(Faction::id).collect(Collectors.toSet());

        for (Achievement achievement : parseAll("achievements", new AchievementParser(), false)) {
            String title = achievement.rewards().title();
            assertTrue(title == null || titles.contains(title), achievement.id() + " da un título que no existe: " + title);
            achievement.criteria().stream()
                    .filter(c -> c.type() == TriggerType.REPUTATION)
                    .forEach(c -> assertTrue(factionIds.contains(c.key()), achievement.id() + ": facción " + c.key()));
        }

        for (Faction faction : factions) {
            faction.rivals().forEach(rival -> assertTrue(factionIds.contains(rival),
                    faction.id() + " tiene como rival una facción que no existe: " + rival));
            faction.ranks().forEach(rank -> {
                String title = rank.rewards().title();
                assertTrue(title == null || titles.contains(title), faction.id() + "/" + rank.id() + ": " + title);
            });
        }

        for (JobEvolution evolution : parseAll("job-evolutions", new JobEvolutionParser(), false)) {
            String title = evolution.rewards().title();
            assertTrue(title == null || titles.contains(title), evolution.id() + ": " + title);
        }
    }

    @Test
    void newFieldsAreRead() {

        Achievement explorer = parseAll("achievements", new AchievementParser(), false).stream()
                .filter(a -> a.id().equals("explorador_incansable")).findFirst().orElseThrow();
        assertEquals(TriggerType.VISIT_BIOME, explorer.criteria().getFirst().type());
        assertEquals(2.0, explorer.rewards().stats().get("speed"));

        Faction kingdom = parseAll("factions", new FactionParser(), false).stream()
                .filter(f -> f.id().equals("kingdom")).findFirst().orElseThrow();
        assertEquals(6, kingdom.ranks().size());
        assertEquals(-50, kingdom.sources().stream()
                .filter(s -> "VILLAGER".equals(s.target())).findFirst().orElseThrow().amount());

        Title veteran = parseAll("titles", new TitleParser(), false).stream()
                .filter(t -> t.id().equals("veterano")).findFirst().orElseThrow();
        assertTrue(veteran.unlocksAutomatically());
        assertEquals(50, veteran.requirements().level());
    }

}
