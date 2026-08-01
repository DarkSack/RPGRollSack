package com.sack.rpgroll.gameplay.hud;

import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Muestra un BossBar persistente con la salud y maná actuales del jugador,
 * ya que RPGRoll rastrea salud/maná como recursos propios independientes
 * de la barra de corazones de Minecraft (ver {@link CombatStats}).
 * <p>
 * El progreso de la barra refleja el porcentaje de salud; el maná se
 * muestra como texto en el título.
 */
public class PlayerResourceBar {

    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();

    public void show(Player player, RPGPlayer rpgPlayer) {
        BossBar bar = bars.computeIfAbsent(player.getUniqueId(),
                uuid -> BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10));

        player.showBossBar(bar);
        update(player, rpgPlayer);
    }

    public void update(Player player, RPGPlayer rpgPlayer) {

        BossBar bar = bars.get(player.getUniqueId());
        if (bar == null) {
            return;
        }

        CombatStats stats = rpgPlayer.getCombatStats();

        float progress = stats.maxHealth() <= 0
                ? 0f
                : clamp((float) stats.currentHealth() / (float) stats.maxHealth());

        bar.progress(progress);
        bar.color(progress <= 0.25f ? BossBar.Color.RED : BossBar.Color.WHITE);

        bar.name(Component.text("❤ " + stats.currentHealth() + "/" + stats.maxHealth(), NamedTextColor.RED)
                .append(Component.text("   ✦ " + stats.currentMana() + "/" + stats.maxMana(), NamedTextColor.AQUA)));
    }

    public void remove(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

}
