package com.sack.rpgroll.gameplay.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.gameplay.combat.CombatTracker;
import com.sack.rpgroll.gameplay.hud.PlayerResourceBar;
import com.sack.rpgroll.gameplay.trait.PassiveEffects;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.cache.PlayerCache;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El daño y la defensa de los rasgos llegan al golpe: sin crítico ni evasión, para que no haya azar. */
class CombatEffectsListenerTest {

    private PlayerManager players;
    private PassiveEffects passive;
    private CombatEffectsListener listener;
    private Player bukkitPlayer;
    private RPGPlayer rpgPlayer;

    @BeforeEach
    void setUp() {
        players = mock(PlayerManager.class);
        passive = mock(PassiveEffects.class);
        bukkitPlayer = mock(Player.class);
        rpgPlayer = mock(RPGPlayer.class);

        when(bukkitPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(players.getPlayer(any())).thenReturn(Optional.of(rpgPlayer));
        when(players.getCache()).thenReturn(mock(PlayerCache.class));
        when(rpgPlayer.updateCombatStats(any())).thenReturn(rpgPlayer);

        listener = new CombatEffectsListener(players, mock(CombatTracker.class), mock(PlayerResourceBar.class),
                mock(LangManager.class), passive);
    }

    /** Vida 100, armadura 50 (la mitad del daño pasa), sin evasión ni crítico. */
    private void stats(double damage, double defense, double evasion, double critical) {
        when(rpgPlayer.getCombatStats()).thenReturn(new CombatStats(100, 100, 100, 100, 50.0, 0.0, 0.0, 1.5));
        when(passive.totals(rpgPlayer)).thenReturn(new PassiveEffects.Totals(0, damage, defense, 0, 0, evasion, critical, 0));
    }

    private static double near(double expected) {
        return doubleThat(value -> Math.abs(value - expected) < 1e-9);
    }

    @Test
    void damageBonusRaisesTheHitAndAPenaltyLowersIt() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(bukkitPlayer);
        when(event.getDamage()).thenReturn(10.0);

        stats(0.25, 0, 0, 0);
        listener.onCriticalHit(event);
        verify(event).setDamage(near(12.5));

        stats(-0.10, 0, 0, 0);
        listener.onCriticalHit(event);
        verify(event).setDamage(near(9.0));
    }

    @Test
    void aSureCriticalMultipliesTheBoostedHit() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(bukkitPlayer);
        when(event.getDamage()).thenReturn(10.0);

        stats(0, 0, 0, 1.0);
        listener.onCriticalHit(event);

        verify(event).setDamage(near(15.0));
    }

    @Test
    void defenseBonusCutsTheDamageTakenAndAPenaltyRaisesIt() {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(bukkitPlayer);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        when(event.getDamage()).thenReturn(20.0);

        stats(0, 0.20, 0, 0);
        listener.onDamageTaken(event);
        verify(event).setDamage(near(8.0));

        stats(0, -0.10, 0, 0);
        listener.onDamageTaken(event);
        verify(event).setDamage(near(11.0));
    }

    @Test
    void aSureEvasionCancelsTheHit() {
        EntityDamageEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(bukkitPlayer);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        when(event.getDamage()).thenReturn(20.0);

        stats(0, 0, 1.0, 0);
        listener.onDamageTaken(event);

        verify(event).setCancelled(true);
        verify(event, never()).setDamage(anyDouble());
    }

    @Test
    void thirstOrAFallIsNeitherDodgedNorCombat() {
        CombatTracker tracker = mock(CombatTracker.class);
        listener = new CombatEffectsListener(players, tracker, mock(PlayerResourceBar.class),
                mock(LangManager.class), passive);
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(bukkitPlayer);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.CUSTOM);
        when(event.getDamage()).thenReturn(1.0);

        stats(0, 0, 1.0, 0);
        listener.onDamageTaken(event);

        verify(event, never()).setCancelled(true);
        verify(tracker, never()).markInCombat(any());
    }

    @Test
    void theHealthLostIsWhatGetsPastArmorAndShield() {
        EntityDamageEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(bukkitPlayer);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        when(event.getDamage()).thenReturn(20.0);
        stats(0, 0, 0, 0);

        when(event.getFinalDamage()).thenReturn(4.0);
        listener.onDamageTaken(event);
        verify(rpgPlayer).updateCombatStats(argThat(stats -> stats.currentHealth() == 96));

        when(event.getFinalDamage()).thenReturn(0.0); // bloqueado con el escudo
        listener.onDamageTaken(event);
        verify(rpgPlayer).updateCombatStats(argThat(stats -> stats.currentHealth() == 100));
    }

    @Test
    void aHealingPotionHealsTheSameShareOfRpgHealth() {
        EntityRegainHealthEvent event = mock(EntityRegainHealthEvent.class);
        when(bukkitPlayer.getMaxHealth()).thenReturn(20.0);
        when(event.getEntity()).thenReturn(bukkitPlayer);
        when(event.getRegainReason()).thenReturn(EntityRegainHealthEvent.RegainReason.MAGIC);
        when(event.getAmount()).thenReturn(5.0);
        when(rpgPlayer.getCombatStats()).thenReturn(new CombatStats(100, 40, 100, 100, 50.0, 0.0, 0.0, 1.5));

        listener.onHeal(event);
        verify(rpgPlayer).updateCombatStats(argThat(stats -> stats.currentHealth() == 65));

        when(event.getRegainReason()).thenReturn(EntityRegainHealthEvent.RegainReason.SATIATED);
        listener.onHeal(event);
        verify(rpgPlayer).updateCombatStats(any()); // la comida no suma: sigue siendo una sola vez
    }

}
