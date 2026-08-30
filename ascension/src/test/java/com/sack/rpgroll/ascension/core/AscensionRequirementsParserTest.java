package com.sack.rpgroll.ascension.core;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AscensionRequirementsParserTest {

    private static YamlConfiguration yaml(String raw) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(raw);
        return configuration;
    }

    @Test
    void nullSectionYieldsNoRequirements() {
        AscensionRequirements requirements = AscensionRequirementsParser.parse(null);

        assertEquals(0, requirements.level());
        assertTrue(requirements.completedQuests().isEmpty());
    }

    @Test
    void emptySectionDefaultsEveryFieldToNeutral() throws Exception {
        var section = yaml("requirements: {}").getConfigurationSection("requirements");

        AscensionRequirements requirements = AscensionRequirementsParser.parse(section);

        assertEquals(0, requirements.level());
        assertEquals(0, requirements.prestige());
        assertNull(requirements.trait());
        assertTrue(requirements.completedQuests().isEmpty());
        assertTrue(requirements.reputation().isEmpty());
    }

    @Test
    void allScalarFieldsAreRead() throws Exception {
        var section = yaml("""
                requirements:
                  level: 40
                  prestige: 2
                  trait: fearless
                """).getConfigurationSection("requirements");

        AscensionRequirements requirements = AscensionRequirementsParser.parse(section);

        assertEquals(40, requirements.level());
        assertEquals(2, requirements.prestige());
        assertEquals("fearless", requirements.trait());
    }

    @Test
    void completedQuestsListIsRead() throws Exception {
        var section = yaml("""
                requirements:
                  completed-quests:
                    - intro
                    - trial_of_fire
                """).getConfigurationSection("requirements");

        assertEquals(List.of("intro", "trial_of_fire"),
                AscensionRequirementsParser.parse(section).completedQuests());
    }

    @Test
    void reputationSectionIsFlattenedIntoFactionToAmountMap() throws Exception {
        var section = yaml("""
                requirements:
                  reputation:
                    mages_guild: 500
                    thieves_guild: -100
                """).getConfigurationSection("requirements");

        assertEquals(Map.of("mages_guild", 500, "thieves_guild", -100),
                AscensionRequirementsParser.parse(section).reputation());
    }

    @Test
    void missingReputationSectionYieldsEmptyMap() throws Exception {
        var section = yaml("""
                requirements:
                  level: 10
                """).getConfigurationSection("requirements");

        assertTrue(AscensionRequirementsParser.parse(section).reputation().isEmpty());
    }
}
