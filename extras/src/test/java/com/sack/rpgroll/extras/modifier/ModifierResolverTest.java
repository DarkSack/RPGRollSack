package com.sack.rpgroll.extras.modifier;

import com.sack.rpgroll.common.character.RPGCharacters;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModifierResolverTest {

    private RPGCharacters characters;
    private Player bukkitPlayer;
    private ModifierManager modifierManager;
    private ModifierResolver resolver;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        characters = mock(RPGCharacters.class);
        bukkitPlayer = mock(Player.class);
        modifierManager = mock(ModifierManager.class);
        resolver = new ModifierResolver(modifierManager, () -> Optional.of(characters));

        uuid = UUID.randomUUID();
        when(bukkitPlayer.getUniqueId()).thenReturn(uuid);
        when(characters.race(uuid)).thenReturn(Optional.empty());
        when(characters.playerClass(uuid)).thenReturn(Optional.empty());
        when(characters.activeJobs(uuid)).thenReturn(Set.of());
    }

    @Test
    void sumReturnsZeroWhenCorePluginIsAbsent() {
        ModifierResolver withoutCore = new ModifierResolver(modifierManager, Optional::empty);

        assertEquals(0.0, withoutCore.sum(bukkitPlayer, "cold_resistance"));
        assertEquals(1.0, withoutCore.multiplier(bukkitPlayer, "stat_max"));
    }

    @Test
    void sumReturnsZeroWhenPlayerHasNoRpgRecord() {
        assertEquals(0.0, resolver.sum(bukkitPlayer, "cold_resistance"));
    }

    @Test
    void sumAddsRaceClassAndAllActiveJobModifiers() {
        when(characters.race(uuid)).thenReturn(Optional.of("elf"));
        when(characters.playerClass(uuid)).thenReturn(Optional.of("mage"));
        when(characters.activeJobs(uuid)).thenReturn(Set.of("miner", "fisher"));

        when(modifierManager.get("elf")).thenReturn(Optional.of(
                new ModifierSet("elf", ModifierSourceType.RACE, Map.of("cold_resistance", 0.1))));
        when(modifierManager.get("mage")).thenReturn(Optional.of(
                new ModifierSet("mage", ModifierSourceType.CLASS, Map.of("cold_resistance", 0.2))));
        when(modifierManager.get("miner")).thenReturn(Optional.of(
                new ModifierSet("miner", ModifierSourceType.JOB, Map.of("cold_resistance", 0.05))));
        when(modifierManager.get("fisher")).thenReturn(Optional.of(
                new ModifierSet("fisher", ModifierSourceType.JOB, Map.of("cold_resistance", 0.05))));

        assertEquals(0.4, resolver.sum(bukkitPlayer, "cold_resistance"), 1e-9);
        assertEquals(1.4, resolver.multiplier(bukkitPlayer, "cold_resistance"), 1e-9);
    }

    @Test
    void sumIgnoresModifierSetWhenTypeDoesNotMatchExpectedSourceType() {
        when(characters.race(uuid)).thenReturn(Optional.of("elf"));

        when(modifierManager.get("elf")).thenReturn(Optional.of(
                new ModifierSet("elf", ModifierSourceType.CLASS, Map.of("cold_resistance", 0.5))));

        assertEquals(0.0, resolver.sum(bukkitPlayer, "cold_resistance"));
    }

    @Test
    void sumTreatsMissingKeyAsZero() {
        when(characters.race(uuid)).thenReturn(Optional.of("elf"));

        when(modifierManager.get("elf")).thenReturn(Optional.of(
                new ModifierSet("elf", ModifierSourceType.RACE, Map.of("other_key", 0.5))));

        assertEquals(0.0, resolver.sum(bukkitPlayer, "cold_resistance"));
    }

    @Test
    void multiplierIsAlwaysOnePlusSumEvenWhenSumIsNegative() {
        when(characters.race(uuid)).thenReturn(Optional.of("elf"));

        when(modifierManager.get("elf")).thenReturn(Optional.of(
                new ModifierSet("elf", ModifierSourceType.RACE, Map.of("stat_max", -0.3))));

        assertEquals(0.7, resolver.multiplier(bukkitPlayer, "stat_max"), 1e-9);
    }
}
