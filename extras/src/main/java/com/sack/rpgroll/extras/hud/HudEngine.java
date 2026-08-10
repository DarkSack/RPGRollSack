package com.sack.rpgroll.extras.hud;

import com.sack.rpgroll.extras.stat.StatEngine;
import com.sack.rpgroll.extras.stat.StatManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * HUD por actionbar (sección 25/26) — deliberadamente NO reimplementa un
 * scoreboard sidebar (RPGRoll-TAB ya tiene uno mucho más completo); en su
 * lugar, sección 13, Extras expone sus stats como placeholders para que
 * TAB los muestre en su propio scoreboard/bossbar si está instalado.
 */
public class HudEngine {

    private final Plugin plugin;
    private final StatManager statManager;
    private final StatEngine statEngine;
    private final HudSettings settings;

    private BukkitTask task;

    public HudEngine(Plugin plugin, StatManager statManager, StatEngine statEngine, HudSettings settings) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.statEngine = statEngine;
        this.settings = settings;
    }

    public void start() {

        if (!settings.enabled() || settings.lines().isEmpty()) {
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, settings.intervalTicks(), settings.intervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(render(player));
        }
    }

    private Component render(Player player) {

        Component result = Component.empty();
        boolean first = true;

        for (HudLineFormat line : settings.lines()) {

            double value = statEngine.get(player, line.statId());
            double max = statManager.get(line.statId()).map(def -> def.max()).orElse(100.0);

            String rendered = line.format()
                    .replace("{value}", String.valueOf(Math.round(value)))
                    .replace("{max}", String.valueOf(Math.round(max)));

            if (line.bar()) {
                rendered = rendered.replace("{bar}", renderBar(value, max, line));
            }

            if (!first) {
                result = result.append(ComponentUtils.parse(settings.separator()));
            }

            result = result.append(ComponentUtils.parse(rendered));
            first = false;
        }

        return result;
    }

    private String renderBar(double value, double max, HudLineFormat line) {

        int filled = max <= 0 ? 0 : (int) Math.round((value / max) * line.barLength());
        filled = Math.max(0, Math.min(line.barLength(), filled));

        StringBuilder bar = new StringBuilder(line.barLength());

        for (int i = 0; i < line.barLength(); i++) {
            bar.append(i < filled ? line.filledChar() : line.emptyChar());
        }

        return bar.toString();
    }

}
