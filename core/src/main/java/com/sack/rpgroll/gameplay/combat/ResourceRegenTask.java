package com.sack.rpgroll.gameplay.combat;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.gameplay.hud.PlayerResourceBar;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.cache.PlayerCache;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Regenera pasivamente un porcentaje de la salud/maná máximos de cada
 * jugador conectado, cada cierto intervalo (ver {@code combat} en
 * gameplay.yml). Respeta {@code combat.natural_regen_in_combat}: si es
 * false, no regenera mientras el jugador esté en combate reciente.
 * <p>
 * Solo actualiza el caché en memoria — la persistencia a BD ocurre en los
 * puntos ya existentes (logout, shutdown), para no golpear la base de
 * datos en cada tick de regeneración.
 */
public class ResourceRegenTask extends BukkitRunnable {

    private final RPGRoll plugin;
    private final PlayerManager playerManager;
    private final CombatTracker combatTracker;
    private final PlayerResourceBar resourceBar;

    private final double healthRegenPercent;
    private final double manaRegenPercent;
    private final int combatDurationSeconds;
    private final boolean regenInCombat;

    public ResourceRegenTask(RPGRoll plugin, PlayerManager playerManager, CombatTracker combatTracker,
            PlayerResourceBar resourceBar, double healthRegenPercent, double manaRegenPercent,
            int combatDurationSeconds, boolean regenInCombat) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.combatTracker = combatTracker;
        this.resourceBar = resourceBar;
        this.healthRegenPercent = healthRegenPercent;
        this.manaRegenPercent = manaRegenPercent;
        this.combatDurationSeconds = combatDurationSeconds;
        this.regenInCombat = regenInCombat;
    }

    public BukkitTask start(long periodTicks) {
        return runTaskTimer(plugin, periodTicks, periodTicks);
    }

    @Override
    public void run() {

        PlayerCache cache = playerManager.getCache();

        for (RPGPlayer rpgPlayer : cache.getAll()) {

            Player bukkitPlayer = Bukkit.getPlayer(rpgPlayer.getUUID());
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                continue;
            }

            if (!regenInCombat && combatTracker.isInCombat(rpgPlayer.getUUID(), combatDurationSeconds)) {
                continue;
            }

            CombatStats stats = rpgPlayer.getCombatStats();

            int healthGain = (int) Math.ceil(stats.maxHealth() * (healthRegenPercent / 100.0));
            int manaGain = (int) Math.ceil(stats.maxMana() * (manaRegenPercent / 100.0));

            if (stats.currentHealth() >= stats.maxHealth() && stats.currentMana() >= stats.maxMana()) {
                continue;
            }

            CombatStats regenerated = stats
                    .withHealth(stats.currentHealth() + healthGain)
                    .withMana(stats.currentMana() + manaGain);

            RPGPlayer updated = rpgPlayer.updateCombatStats(regenerated);
            cache.update(updated);

            resourceBar.update(bukkitPlayer, updated);
        }

    }

}
