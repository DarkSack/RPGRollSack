package com.sack.rpgroll.gameplay.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.gameplay.combat.CombatTracker;
import com.sack.rpgroll.gameplay.hud.PlayerResourceBar;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Conecta armorRating/evasionChance/criticalChance/criticalMultiplier
 * (ver {@link CombatStats}) con el combate real, y hace que la salud RPG
 * del jugador ({@code currentHealth}) se consuma con el daño recibido —
 * es un recurso propio de RPGRoll, independiente de los corazones de
 * Minecraft, que ya maneja su propia muerte/respawn de forma vanilla.
 */
public class CombatEffectsListener implements Listener {

    private static final int DOWNED_EFFECT_DURATION_TICKS = 5 * 20;
    private static final double DOWNED_RECOVERY_FRACTION = 0.25;

    private final PlayerManager playerManager;
    private final CombatTracker combatTracker;
    private final PlayerResourceBar resourceBar;
    private final LangManager lang;

    public CombatEffectsListener(PlayerManager playerManager, CombatTracker combatTracker,
            PlayerResourceBar resourceBar, LangManager lang) {
        this.playerManager = playerManager;
        this.combatTracker = combatTracker;
        this.resourceBar = resourceBar;
        this.lang = lang;
    }

    /**
     * Aplica el multiplicador de crítico del atacante ANTES de que el
     * daño sea reducido por la armadura del defensor (ver
     * {@link #onDamageTaken(EntityDamageEvent)}, que corre en una
     * prioridad posterior).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCriticalHit(EntityDamageByEntityEvent event) {

        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }

        Optional<RPGPlayer> rpgAttacker = playerManager.getPlayer(attacker.getUniqueId());
        if (rpgAttacker.isEmpty()) {
            return;
        }

        combatTracker.markInCombat(attacker.getUniqueId());

        CombatStats stats = rpgAttacker.get().getCombatStats();

        if (ThreadLocalRandom.current().nextDouble() < stats.criticalChance()) {
            event.setDamage(event.getDamage() * stats.criticalMultiplier());
            attacker.sendActionBar(lang.component("combat_effects_listener.critical_hit"));
        }

    }

    /**
     * Aplica evasión y reducción por armadura al jugador que recibe daño,
     * y descuenta el resultado de su salud RPG (currentHealth).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        Optional<RPGPlayer> rpgVictimOpt = playerManager.getPlayer(victim.getUniqueId());
        if (rpgVictimOpt.isEmpty()) {
            return;
        }

        combatTracker.markInCombat(victim.getUniqueId());

        RPGPlayer rpgVictim = rpgVictimOpt.get();
        CombatStats stats = rpgVictim.getCombatStats();

        if (ThreadLocalRandom.current().nextDouble() < stats.evasionChance()) {
            event.setCancelled(true);
            victim.sendActionBar(lang.component("combat_effects_listener.evaded"));
            return;
        }

        double armorReduction = stats.armorRating() / (stats.armorRating() + 50.0);
        double reducedDamage = event.getDamage() * (1 - armorReduction);
        event.setDamage(reducedDamage);

        int newHealth = (int) Math.round(stats.currentHealth() - reducedDamage);
        CombatStats updatedStats;

        if (newHealth <= 0) {
            updatedStats = stats.withHealth((int) Math.ceil(stats.maxHealth() * DOWNED_RECOVERY_FRACTION));
            victim.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, DOWNED_EFFECT_DURATION_TICKS, 1));
            victim.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS, DOWNED_EFFECT_DURATION_TICKS, 1));
            victim.sendMessage(lang.component("combat_effects_listener.downed"));
        } else {
            updatedStats = stats.withHealth(newHealth);
        }

        RPGPlayer updatedVictim = rpgVictim.updateCombatStats(updatedStats);
        playerManager.getCache().update(updatedVictim);
        resourceBar.update(victim, updatedVictim);

    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player player) {
            return player;
        }

        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }

        return null;

    }

}
