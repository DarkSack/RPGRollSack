package com.sack.rpgroll.pass.requirement;

import com.sack.rpgroll.pass.season.Season;
import com.sack.rpgroll.pass.season.SeasonLoader;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelRequirementsTest {

    @Test
    void durationsAcceptMinutesHoursDaysAndMixes() {

        assertEquals(90, LevelRequirements.minutes("90"));
        assertEquals(30, LevelRequirements.minutes("30m"));
        assertEquals(300, LevelRequirements.minutes("5h"));
        assertEquals(1440, LevelRequirements.minutes("1d"));
        assertEquals(150, LevelRequirements.minutes("2h30m"));
        assertEquals(150, LevelRequirements.minutes("2h 30m"));
    }

    @Test
    void durationsRejectWhatTheyDoNotUnderstand() {

        assertThrows(IllegalArgumentException.class, () -> LevelRequirements.minutes("cinco horas"));
        assertThrows(IllegalArgumentException.class, () -> LevelRequirements.minutes("5x"));
        assertThrows(IllegalArgumentException.class, () -> LevelRequirements.minutes("5h y algo"));
    }

    @Test
    void formatIsReadable() {

        assertEquals("0m", LevelRequirements.format(0));
        assertEquals("45m", LevelRequirements.format(45));
        assertEquals("2h 30m", LevelRequirements.format(150));
        assertEquals("1d 1h", LevelRequirements.format(1500));
    }

    @Test
    void noneAndDayOneRequireNothing() {

        assertTrue(LevelRequirements.NONE.isEmpty());
        assertTrue(new LevelRequirements(0, 0, 1, null, 0, 0, 0, List.of(), " ", null).isEmpty());
    }

    @Test
    void permissionNameFallsBackToTheNode() {

        LevelRequirements requirements = new LevelRequirements(0, 0, 0, null, 0, 0, 0, null, "rank.vip", "");

        assertEquals("rank.vip", requirements.permissionName());
        assertFalse(requirements.isEmpty());
    }

    @Test
    void seasonLevelsReadTheirRequirementsBlock() throws InvalidConfigurationException {

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
                "start: 2026-09-25",
                "end: 2026-11-08",
                "levels:",
                "  1:",
                "    free: [money:100]",
                "  10:",
                "    free: [money:100]",
                "    requirements:",
                "      character-level: 15",
                "      playtime: 5h",
                "      season-day: 7",
                "      available-from: 2026-10-15",
                "      votes: 10",
                "      daily-streak: 5",
                "      missions: 20",
                "      quests: [dragon_intro]",
                "      permission: rank.vip",
                "      permission-name: \"&6VIP\"",
                "      volar: true"));

        List<String> warnings = new ArrayList<>();
        Season season = SeasonLoader.parse(yaml, "t1", warnings::add);

        assertTrue(season.level(1).orElseThrow().requirements().isEmpty());

        LevelRequirements requirements = season.level(10).orElseThrow().requirements();
        assertEquals(15, requirements.characterLevel());
        assertEquals(300, requirements.playtimeMinutes());
        assertEquals(7, requirements.seasonDay());
        assertEquals(LocalDate.of(2026, 10, 15), requirements.availableFrom());
        assertEquals(10, requirements.votes());
        assertEquals(5, requirements.dailyStreak());
        assertEquals(20, requirements.missions());
        assertEquals(List.of("dragon_intro"), requirements.quests());
        assertEquals("rank.vip", requirements.permission());
        assertEquals("&6VIP", requirements.permissionName());

        // La clave desconocida se avisa, no tumba el nivel.
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("volar"));
    }

    @Test
    void aBadPlaytimeIsReportedAndIgnored() throws InvalidConfigurationException {

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("playtime: mucho\navailable-from: mañana\n");

        List<String> warnings = new ArrayList<>();
        LevelRequirements requirements = LevelRequirements.parse(yaml, "nivel 3", warnings::add);

        assertEquals(0, requirements.playtimeMinutes());
        assertNull(requirements.availableFrom());
        assertEquals(2, warnings.size());
    }

}
