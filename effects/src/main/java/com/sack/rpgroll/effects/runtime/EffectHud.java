package com.sack.rpgroll.effects.runtime;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.bossbar.BossBar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enseña al jugador qué efectos lleva encima: una barra de jefe por cada
 * efecto {@code visible}, con su color, sus cargas y el tiempo que le queda,
 * vaciándose a medida que se acaba.
 * <p>
 * Antes el campo {@code visible} no se usaba en ningún sitio y el jugador no
 * tenía forma de saber que estaba envenenado salvo por el mensaje del
 * principio. Barra de jefe y no barra de acción porque la de acción ya la
 * ocupa el HUD de RPGRoll-Extras y se pisarían cada segundo.
 */
public class EffectHud {

    private static final long PERIOD_TICKS = 5L;

    private final Plugin plugin;
    private final EffectTracker tracker;
    private final LangManager lang;
    private final Map<UUID, Map<String, BossBar>> bars = new HashMap<>();

    private boolean enabled;
    private int maxBars;
    private BukkitTask task;

    public EffectHud(Plugin plugin, EffectTracker tracker, LangManager lang) {
        this.plugin = plugin;
        this.tracker = tracker;
        this.lang = lang;
    }

    /** Lee {@code hud.*} del config y arranca (o para) la tarea. Se llama al habilitar y en cada reload. */
    public void reload() {

        enabled = plugin.getConfig().getBoolean("hud.bossbar", true);
        maxBars = Math.max(1, plugin.getConfig().getInt("hud.max-bars", 3));

        if (!enabled) {
            stop();
            return;
        }

        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, PERIOD_TICKS, PERIOD_TICKS);
        }
    }

    public void stop() {

        if (task != null) {
            task.cancel();
            task = null;
        }

        for (UUID id : Set.copyOf(bars.keySet())) {
            clear(id);
        }
    }

    private void refreshAll() {

        Set<UUID> online = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            online.add(player.getUniqueId());
            refresh(player);
        }

        for (UUID id : Set.copyOf(bars.keySet())) {
            if (!online.contains(id)) {
                bars.remove(id);
            }
        }
    }

    private void refresh(Player player) {

        List<ActiveEffect> shown = tracker.getActive(player).stream()
                .filter(effect -> effect.definition().visible())
                .sorted(Comparator.comparingInt((ActiveEffect effect) -> effect.definition().priority()).reversed()
                        .thenComparingInt(ActiveEffect::remainingTicks))
                .limit(maxBars)
                .toList();

        Map<String, BossBar> current = bars.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        Set<String> keep = new HashSet<>();

        for (ActiveEffect effect : shown) {

            EffectDefinition definition = effect.definition();
            keep.add(definition.id());

            BossBar bar = current.computeIfAbsent(definition.id(), id -> {
                BossBar created = BossBar.bossBar(ComponentUtils.parse(definition.displayName()), 1f,
                        colorOf(definition.color()), BossBar.Overlay.PROGRESS);
                player.showBossBar(created);
                return created;
            });

            bar.name(ComponentUtils.parse(title(effect)));
            bar.progress(progress(effect));
        }

        for (String id : Set.copyOf(current.keySet())) {
            if (!keep.contains(id)) {
                player.hideBossBar(current.remove(id));
            }
        }
    }

    private String title(ActiveEffect effect) {

        String stacks = effect.stacks() > 1 ? lang.raw("hud.stacks", "stacks", effect.stacks()) : "";

        if (effect.isPermanent()) {
            return lang.raw("hud.title_permanent", "name", effect.definition().displayName(), "stacks", stacks);
        }

        int seconds = (int) Math.ceil(effect.remainingTicks() / 20.0);
        return lang.raw("hud.title", "name", effect.definition().displayName(), "stacks", stacks,
                "seconds", seconds);
    }

    private static float progress(ActiveEffect effect) {

        if (effect.isPermanent()) {
            return 1f;
        }

        float value = effect.remainingTicks() / (float) effect.definition().durationTicks();
        return Math.max(0f, Math.min(1f, value));
    }

    private void clear(UUID id) {

        Map<String, BossBar> playerBars = bars.remove(id);
        Player player = Bukkit.getPlayer(id);

        if (playerBars == null || player == null) {
            return;
        }

        playerBars.values().forEach(player::hideBossBar);
    }

    /** El {@code color} del efecto (nombre de color de chat) llevado a los siete colores de una barra de jefe. */
    static BossBar.Color colorOf(String color) {
        return switch (color == null ? "" : color.trim().toUpperCase(Locale.ROOT)) {
            case "RED", "DARK_RED" -> BossBar.Color.RED;
            case "GOLD", "YELLOW" -> BossBar.Color.YELLOW;
            case "GREEN", "DARK_GREEN" -> BossBar.Color.GREEN;
            case "AQUA", "DARK_AQUA", "BLUE", "DARK_BLUE" -> BossBar.Color.BLUE;
            case "LIGHT_PURPLE" -> BossBar.Color.PINK;
            case "DARK_PURPLE" -> BossBar.Color.PURPLE;
            default -> BossBar.Color.WHITE;
        };
    }

}
