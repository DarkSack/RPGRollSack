package com.sack.rpgroll.extras.modifier;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.core.Bootstrap;
import com.sack.rpgroll.core.ServiceRegistry;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.PlayerJobs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ModifierResolverTest {

    private MockedStatic<Bukkit> bukkitMock;
    private PluginManager pluginManager;
    private RPGRoll rpgRoll;
    private Bootstrap bootstrap;
    private PlayerManager playerManager;
    private Player bukkitPlayer;
    private RPGPlayer rpgPlayer;
    private ModifierManager modifierManager;
    private ModifierResolver resolver;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        pluginManager = mock(PluginManager.class);
        rpgRoll = mock(RPGRoll.class);
        bootstrap = mock(Bootstrap.class);
        ServiceRegistry services = new ServiceRegistry();
        playerManager = mock(PlayerManager.class);
        bukkitPlayer = mock(Player.class);
        rpgPlayer = mock(RPGPlayer.class);
        modifierManager = mock(ModifierManager.class);
        resolver = new ModifierResolver(modifierManager);

        services.register(PlayerManager.class, playerManager);
        when(bootstrap.getServices()).thenReturn(services);
        when(rpgRoll.getBootstrap()).thenReturn(bootstrap);
        when(pluginManager.getPlugin("RPGRoll")).thenReturn(rpgRoll);

        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);

        uuid = UUID.randomUUID();
        when(bukkitPlayer.getUniqueId()).thenReturn(uuid);
        when(playerManager.getPlayer(uuid)).thenReturn(Optional.of(rpgPlayer));
    }

    @AfterEach
    void tearDown() {
        bukkitMock.close();
    }

    @Test
    void sumReturnsZeroWhenCorePluginIsAbsent() {
        when(pluginManager.getPlugin("RPGRoll")).thenReturn(null);

        assertEquals(0.0, resolver.sum(bukkitPlayer, "cold_resistance"));
        assertEquals(1.0, resolver.multiplier(bukkitPlayer, "stat_max"));
    }

    @Test
    void sumReturnsZeroWhenPlayerHasNoRpgRecord() {
        when(playerManager.getPlayer(uuid)).thenReturn(Optional.empty());

        assertEquals(0.0, resolver.sum(bukkitPlayer, "cold_resistance"));
    }

    @Test
    void sumAddsRaceClassAndAllActiveJobModifiers() {
        when(rpgPlayer.getRace()).thenReturn("elf");
        when(rpgPlayer.getPlayerClass()).thenReturn("mage");
        when(rpgPlayer.getJobs()).thenReturn(PlayerJobs.empty().join("miner").join("fisher"));

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
        when(rpgPlayer.getRace()).thenReturn("elf");
        when(rpgPlayer.getPlayerClass()).thenReturn(null);
        when(rpgPlayer.getJobs()).thenReturn(PlayerJobs.empty());

        when(modifierManager.get("elf")).thenReturn(Optional.of(
                new ModifierSet("elf", ModifierSourceType.CLASS, Map.of("cold_resistance", 0.5))));

        assertEquals(0.0, resolver.sum(bukkitPlayer, "cold_resistance"));
    }

    @Test
    void sumTreatsMissingKeyAsZero() {
        when(rpgPlayer.getRace()).thenReturn("elf");
        when(rpgPlayer.getPlayerClass()).thenReturn(null);
        when(rpgPlayer.getJobs()).thenReturn(PlayerJobs.empty());

        when(modifierManager.get("elf")).thenReturn(Optional.of(
                new ModifierSet("elf", ModifierSourceType.RACE, Map.of("other_key", 0.5))));

        assertEquals(0.0, resolver.sum(bukkitPlayer, "cold_resistance"));
    }

    @Test
    void multiplierIsAlwaysOnePlusSumEvenWhenSumIsNegative() {
        when(rpgPlayer.getRace()).thenReturn("elf");
        when(rpgPlayer.getPlayerClass()).thenReturn(null);
        when(rpgPlayer.getJobs()).thenReturn(PlayerJobs.empty());

        when(modifierManager.get("elf")).thenReturn(Optional.of(
                new ModifierSet("elf", ModifierSourceType.RACE, Map.of("stat_max", -0.3))));

        assertEquals(0.7, resolver.multiplier(bukkitPlayer, "stat_max"), 1e-9);
    }
}
