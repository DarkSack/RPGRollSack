package com.sack.rpgroll.extras.condition;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionParserTest {

    private final ConditionParser parser = new ConditionParser();

    @Test
    void parsesFullDefinitionWithAllActionLists() throws Exception {
        YamlConfiguration config = load("""
                id: bleeding
                duration: 200
                damage: 2.0
                interval: 40
                effects:
                  - WEAKNESS:1
                on-apply:
                  - type: MESSAGE
                    value: "you are bleeding"
                on-tick:
                  - type: DAMAGE
                    value: "2"
                on-expire:
                  - type: MESSAGE
                    value: "bleeding stopped"
                """);

        ConditionDefinition definition = parser.parse(config);

        assertEquals("bleeding", definition.id());
        assertEquals(200, definition.durationTicks());
        assertEquals(2.0, definition.periodicDamage());
        assertEquals(40, definition.intervalTicks());
        assertEquals(1, definition.potionEffects().size());
        assertEquals(1, definition.onApply().size());
        assertEquals(1, definition.onTick().size());
        assertEquals(1, definition.onExpire().size());
    }

    @Test
    void missingIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("duration: 100")));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("id: '  '")));
    }

    @Test
    void defaultsApplyWhenOptionalFieldsOmitted() throws Exception {
        ConditionDefinition definition = parser.parse(load("id: minimal"));

        assertEquals(-1, definition.durationTicks());
        assertEquals(0, definition.periodicDamage());
        assertEquals(20, definition.intervalTicks());
        assertTrue(definition.potionEffects().isEmpty());
        assertTrue(definition.onApply().isEmpty());
        assertTrue(definition.onTick().isEmpty());
        assertTrue(definition.onExpire().isEmpty());
    }

    @Test
    void unknownActionTypeInActionListIsSkippedNotThrown() throws Exception {
        ConditionDefinition definition = parser.parse(load("""
                id: with-bad-action
                on-apply:
                  - type: NOT_REAL
                    value: "x"
                """));

        assertTrue(definition.onApply().isEmpty());
    }

    private YamlConfiguration load(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }
}
