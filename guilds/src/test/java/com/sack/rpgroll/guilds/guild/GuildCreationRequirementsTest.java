package com.sack.rpgroll.guilds.guild;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuildCreationRequirementsTest {

    @Test
    void defaultsMatchSpec() {
        GuildCreationRequirements requirements = GuildCreationRequirements.defaults();

        assertEquals(5000, requirements.moneyCost());
        assertEquals(5, requirements.minLevel());
        assertNull(requirements.requiredItem());
    }

    @Test
    void nullSectionFallsBackToDefaults() {
        assertEquals(GuildCreationRequirements.defaults(), GuildCreationRequirements.fromConfig(null));
    }

    @Test
    void fromConfigParsesAllFields() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                requirements:
                  money-cost: 10000
                  min-level: 20
                  required-permission: guild.founder
                  required-quest: quest1
                  required-item: DIAMOND
                  required-item-amount: 5
                """);

        ConfigurationSection section = config.getConfigurationSection("requirements");
        GuildCreationRequirements requirements = GuildCreationRequirements.fromConfig(section);

        assertEquals(10000.0, requirements.moneyCost());
        assertEquals(20, requirements.minLevel());
        assertEquals("guild.founder", requirements.requiredPermission());
        assertEquals("quest1", requirements.requiredQuestId());
        assertEquals(Material.DIAMOND, requirements.requiredItem());
        assertEquals(5, requirements.requiredItemAmount());
    }

    @Test
    void invalidRequiredItemFallsBackToNull() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                requirements:
                  required-item: NOT_A_MATERIAL
                """);

        GuildCreationRequirements requirements = GuildCreationRequirements
                .fromConfig(config.getConfigurationSection("requirements"));

        assertNull(requirements.requiredItem());
    }
}
