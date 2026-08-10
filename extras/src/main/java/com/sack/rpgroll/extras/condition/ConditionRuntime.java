package com.sack.rpgroll.extras.condition;

import com.sack.rpgroll.extras.action.ExtrasActionExecutor;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado runtime de las conditions activas por jugador. Corre un tick
 * liviano (solo comparaciones de módulo sobre mapas en memoria, sin
 * llamadas a Bukkit salvo cuando algo REALMENTE dispara) para poder ofrecer
 * daño/potiones periódicos con precisión — mismo patrón ya usado por el
 * tracking de efectos activos de RPGRoll-Effects.
 */
public class ConditionRuntime {

    private record ActiveCondition(ConditionDefinition definition, long expiresAtTick, long appliedAtTick) {
    }

    private final ExtrasActionExecutor actionExecutor;
    private final Map<UUID, Map<String, ActiveCondition>> active = new ConcurrentHashMap<>();

    private long tickCounter = 0;
    private BukkitTask task;

    public ConditionRuntime(ExtrasActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
    }

    public void start(Plugin plugin) {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {

        tickCounter++;

        for (var playerEntry : new ArrayList<>(active.entrySet())) {

            Player player = org.bukkit.Bukkit.getPlayer(playerEntry.getKey());
            if (player == null) {
                continue;
            }

            for (var conditionEntry : new ArrayList<>(playerEntry.getValue().entrySet())) {
                tickCondition(player, conditionEntry.getValue());
            }
        }
    }

    private void tickCondition(Player player, ActiveCondition activeCondition) {

        ConditionDefinition definition = activeCondition.definition();

        if (definition.durationTicks() >= 0 && tickCounter >= activeCondition.expiresAtTick()) {
            remove(player, definition.id());
            return;
        }

        long elapsed = tickCounter - activeCondition.appliedAtTick();

        if (definition.intervalTicks() > 0 && elapsed % definition.intervalTicks() == 0) {

            applyPotions(player, definition.potionEffects());

            if (definition.periodicDamage() > 0) {
                player.damage(definition.periodicDamage());
            }

            actionExecutor.execute(player, definition.onTick());
        }
    }

    /** @return true si la condition NO estaba ya activa (se acaba de aplicar por primera vez). */
    public boolean apply(Player player, ConditionDefinition definition) {

        Map<String, ActiveCondition> playerConditions = active.computeIfAbsent(
                player.getUniqueId(), id -> new ConcurrentHashMap<>());

        boolean isNew = !playerConditions.containsKey(definition.id());
        long expiresAt = definition.durationTicks() >= 0 ? tickCounter + definition.durationTicks() : -1;

        playerConditions.put(definition.id(), new ActiveCondition(definition, expiresAt, tickCounter));

        if (isNew) {
            actionExecutor.execute(player, definition.onApply());
        }

        return isNew;
    }

    public boolean has(Player player, String conditionId) {

        Map<String, ActiveCondition> playerConditions = active.get(player.getUniqueId());
        return playerConditions != null && playerConditions.containsKey(conditionId);
    }

    public void remove(Player player, String conditionId) {

        Map<String, ActiveCondition> playerConditions = active.get(player.getUniqueId());

        if (playerConditions == null) {
            return;
        }

        ActiveCondition removed = playerConditions.remove(conditionId);

        if (removed != null) {
            actionExecutor.execute(player, removed.definition().onExpire());
        }
    }

    public List<String> activeConditions(Player player) {

        Map<String, ActiveCondition> playerConditions = active.get(player.getUniqueId());
        return playerConditions == null ? List.of() : List.copyOf(playerConditions.keySet());
    }

    public void clear(Player player) {
        active.remove(player.getUniqueId());
    }

    private void applyPotions(Player player, List<String> potionEffects) {

        for (String raw : potionEffects) {

            String[] parts = raw.split(":", 2);
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(parts[0].trim().toLowerCase(Locale.ROOT)));

            if (type == null) {
                continue;
            }

            int amplifier = parts.length >= 2 ? parseIntOrDefault(parts[1], 0) : 0;
            player.addPotionEffect(new PotionEffect(type, 60, amplifier, true, false));
        }
    }

    private int parseIntOrDefault(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
