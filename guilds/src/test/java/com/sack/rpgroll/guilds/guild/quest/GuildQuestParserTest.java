package com.sack.rpgroll.guilds.guild.quest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuildQuestParserTest {

    private final GuildQuestParser parser = new GuildQuestParser();

    private YamlConfiguration load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @Test
    void missingIdThrows() {
        YamlConfiguration config = load("type: GATHER_RESOURCE");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void invalidTypeThrows() {
        YamlConfiguration config = load("""
                id: quest1
                type: NOT_A_TYPE
                """);
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void missingTypeDefaultsToGatherResource() throws Exception {
        YamlConfiguration config = load("id: quest1");
        assertEquals(GuildQuestType.GATHER_RESOURCE, parser.parse(config).type());
    }

    @Test
    void parsesAllFieldsWithDefaults() throws Exception {
        YamlConfiguration config = load("""
                id: quest1
                type: defeat_boss
                target-reference: skeleton-king
                target-amount: 3
                reward-money: 500
                reward-xp: 100
                min-guild-level: 10
                """);

        GuildQuestDefinition def = parser.parse(config);

        assertEquals("quest1", def.id());
        assertEquals(GuildQuestType.DEFEAT_BOSS, def.type());
        assertEquals("skeleton-king", def.targetReference());
        assertEquals(3, def.targetAmount());
        assertEquals(500.0, def.rewardMoney());
        assertEquals(100, def.rewardXp());
        assertEquals(10, def.minGuildLevel());
    }

    @Test
    void missingOptionalFieldsFallBackToDefaults() throws Exception {
        YamlConfiguration config = load("id: quest1");
        GuildQuestDefinition def = parser.parse(config);

        assertEquals("quest1", def.displayName());
        assertEquals("", def.description());
        assertEquals(1, def.targetAmount());
        assertEquals(0.0, def.rewardMoney());
        assertEquals(0, def.rewardXp());
        assertEquals(1, def.minGuildLevel());
    }
}
