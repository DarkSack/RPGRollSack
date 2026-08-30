package com.sack.rpgroll.gameplay.hud;

import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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
 * <p>
 * Como los corazones vanilla de Minecraft son un pool de vida totalmente
 * aparte de {@link CombatStats} (el daño vanilla los reduce en paralelo,
 * ver {@code CombatEffectsListener}), {@link #update} también sincroniza
 * los corazones vanilla para que reflejen el mismo porcentaje que el
 * número mostrado en la BossBar — si no, ambos números divergen y el
 * jugador ve una vida "incoherente".
 */
public class PlayerResourceBar {

    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final Plugin plugin;

    public PlayerResourceBar(Plugin plugin) {
        this.plugin = plugin;
    }

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

        syncVanillaHealth(player, progress);
    }

    /**
     * Aplica el mismo porcentaje de vida a los corazones vanilla del
     * jugador. Se difiere un tick porque, cuando esto se llama desde un
     * handler de {@code EntityDamageEvent} sin cancelar (solo reduce
     * {@code event.setDamage}), Bukkit todavía va a aplicar el daño
     * vanilla original DESPUÉS de que el listener retorne — si
     * seteáramos la vida ya mismo, ese daño posterior la pisaría.
     */
    private void syncVanillaHealth(Player player, float progress) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            double targetHealth = Math.max(0.0, Math.min(maxHealth, maxHealth * progress));
            if (targetHealth <= 0.0) {
                return;
            }
            player.setHealth(targetHealth);
        });
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
