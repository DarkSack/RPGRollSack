package com.sack.rpgroll.tab.animation;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Reloj compartido para todas las animaciones (header/footer/scoreboard-
 * title/bossbar-title). Un único {@link BukkitTask} incrementa un contador
 * de ticks — cada {@link AnimationDefinition} calcula su frame actual a
 * partir de ese contador ({@link AnimationDefinition#frameAt(long)}), sin
 * que cada elemento animado necesite su propio scheduler.
 * <p>
 * Los elementos NO animados no pagan ningún costo de este engine: solo se
 * consulta cuando hay una {@link AnimationDefinition} asociada.
 */
public class AnimationEngine {

    private static final long TICK_PERIOD = 1L;

    private final Plugin plugin;
    private long tickCounter = 0L;
    private BukkitTask task;

    public AnimationEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {

        if (task != null) {
            return;
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tickCounter++, TICK_PERIOD, TICK_PERIOD);
    }

    public void stop() {

        if (task == null) {
            return;
        }

        task.cancel();
        task = null;
    }

    public long tickCounter() {
        return tickCounter;
    }

    public String currentFrame(AnimationDefinition animation) {
        return animation == null ? null : animation.frameAt(tickCounter);
    }

}
