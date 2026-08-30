package com.sack.rpgroll.crates.core;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrateParserTest {

    private final CrateParser parser = new CrateParser();

    @Test
    void parsesFullDefinitionWithRewardsAndActions() throws Exception {
        YamlConfiguration config = load("""
                id: legendary
                display-name: "Legendary Crate"
                gui-title: "Open Me"
                require-key: true
                key:
                  material: TRIPWIRE_HOOK
                  name: "Legendary Key"
                  lore:
                    - "shiny"
                hologram:
                  - "line1"
                rewards:
                  - id: reward1
                    display-name: "Reward One"
                    icon: DIAMOND
                    weight: "3.5"
                    announce: "true"
                    lore:
                      - "cool item"
                    actions:
                      - type: MESSAGE
                        value: "you won!"
                      - type: NOT_A_REAL_TYPE
                        value: "skipped"
                """);

        Crate crate = parser.parse(config);

        assertEquals("legendary", crate.id());
        assertEquals("Legendary Crate", crate.displayName());
        assertEquals("Open Me", crate.guiTitle());
        assertTrue(crate.requireKey());
        assertEquals(Material.TRIPWIRE_HOOK, crate.keyMaterial());
        assertEquals("Legendary Key", crate.keyDisplayName());
        assertEquals(1, crate.keyLore().size());
        assertEquals(1, crate.hologramLines().size());

        assertEquals(1, crate.rewards().size());
        CrateReward reward = crate.rewards().get(0);
        assertEquals("reward1", reward.id());
        assertEquals("Reward One", reward.displayName());
        assertEquals(Material.DIAMOND, reward.icon());
        assertEquals(3.5, reward.weight());
        assertTrue(reward.announceGlobally());
        assertEquals(1, reward.lore().size());
        assertEquals(1, reward.actions().size());
        assertEquals(CrateAction.CrateActionType.MESSAGE, reward.actions().get(0).type());
    }

    @Test
    void missingIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
                rewards:
                  - id: r1
                """)));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
                id: "   "
                rewards:
                  - id: r1
                """)));
    }

    @Test
    void noRewardsThrowsBecauseCrateRequiresAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("id: empty-crate")));
    }

    @Test
    void rewardEntryMissingIdIsSkipped() throws Exception {
        Crate crate = parser.parse(load("""
                id: mixed
                rewards:
                  - display-name: "no id here"
                  - id: valid
                """));

        assertEquals(1, crate.rewards().size());
        assertEquals("valid", crate.rewards().get(0).id());
    }

    @Test
    void rewardDefaultsApplyWhenOptionalFieldsOmitted() throws Exception {
        Crate crate = parser.parse(load("""
                id: minimal
                rewards:
                  - id: r1
                """));

        CrateReward reward = crate.rewards().get(0);
        assertEquals("r1", reward.id());
        assertEquals(Material.PAPER, reward.icon());
        assertEquals(1.0, reward.weight());
        assertFalse(reward.announceGlobally());
        assertTrue(reward.lore().isEmpty());
        assertTrue(reward.actions().isEmpty());
    }

    @Test
    void invalidRewardWeightPropagatesAsIllegalArgumentWithCrateContext() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
                id: bad-weight
                rewards:
                  - id: r1
                    weight: "0"
                """)));

        assertTrue(ex.getMessage().contains("bad-weight"));
    }

    @Test
    void invalidKeyMaterialFallsBackToTripwireHook() throws Exception {
        Crate crate = parser.parse(load("""
                id: bad-key
                key:
                  material: NOT_A_MATERIAL
                rewards:
                  - id: r1
                """));

        assertEquals(Material.TRIPWIRE_HOOK, crate.keyMaterial());
    }

    private YamlConfiguration load(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }
}
