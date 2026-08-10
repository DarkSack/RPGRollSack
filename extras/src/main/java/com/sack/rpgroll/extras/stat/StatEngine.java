package com.sack.rpgroll.extras.stat;

import com.sack.rpgroll.extras.action.ExtrasActionExecutor;
import com.sack.rpgroll.extras.condition.ConditionManager;
import com.sack.rpgroll.extras.condition.ConditionRuntime;
import com.sack.rpgroll.extras.expression.NumericComparison;
import com.sack.rpgroll.extras.expression.RateConditionEvaluator;
import com.sack.rpgroll.extras.modifier.ModifierResolver;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor genérico de stats (sección 1/27): el mismo código atiende sed,
 * stamina, fatiga, oxígeno, estrés o cualquier need custom — el
 * comportamiento entero (decay/regen/consumption/thresholds) viene del
 * YAML. Decay y regeneración corren en tareas programadas al intervalo
 * QUE CADA STAT DECLARA (no un tick global), y consumo/ajustes puntuales
 * son siempre por evento (acción del jugador o llamada de otro addon vía API).
 */
public class StatEngine {

    private static final int THRESHOLD_CHECK_INTERVAL_TICKS = 20;

    private final Plugin plugin;
    private final StatManager statManager;
    private final RateConditionEvaluator rateConditionEvaluator;
    private final ExtrasActionExecutor actionExecutor;

    private ConditionManager conditionManager;
    private ConditionRuntime conditionRuntime;
    private ModifierResolver modifierResolver;

    private final Map<UUID, Map<String, Double>> values = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> activeThresholds = new ConcurrentHashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    public StatEngine(Plugin plugin, StatManager statManager, RateConditionEvaluator rateConditionEvaluator,
            ExtrasActionExecutor actionExecutor) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.rateConditionEvaluator = rateConditionEvaluator;
        this.actionExecutor = actionExecutor;
    }

    /** Inyección tardía — ConditionManager/Runtime se crean después de StatEngine para evitar un ciclo de constructores. */
    public void linkConditions(ConditionManager conditionManager, ConditionRuntime conditionRuntime) {
        this.conditionManager = conditionManager;
        this.conditionRuntime = conditionRuntime;
    }

    /** Inyección tardía — modificadores de raza/clase/job (sección 18), opcional. */
    public void linkModifiers(ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    public void start() {

        for (StatDefinition stat : statManager.getAll()) {

            if (!stat.enabled()) {
                continue;
            }

            if (stat.decay() != null) {
                tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> tickDecay(stat),
                        stat.decay().intervalTicks(), stat.decay().intervalTicks()));
            }

            if (stat.regeneration() != null) {
                tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> tickRegeneration(stat),
                        stat.regeneration().intervalTicks(), stat.regeneration().intervalTicks()));
            }
        }

        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::tickThresholds,
                THRESHOLD_CHECK_INTERVAL_TICKS, THRESHOLD_CHECK_INTERVAL_TICKS));
    }

    public void stop() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    private void tickDecay(StatDefinition stat) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            adjust(player, stat.id(), -stat.decay().amount() * rateMultiplier(player, stat.id()));
        }
    }

    private void tickRegeneration(StatDefinition stat) {

        for (Player player : Bukkit.getOnlinePlayers()) {

            double delta = 0;

            for (RateRule rule : stat.regeneration().rules()) {
                if (rateConditionEvaluator.matches(rule.condition(), player)) {
                    delta += rule.amount();
                }
            }

            if (delta != 0) {
                adjust(player, stat.id(), delta * rateMultiplier(player, stat.id()));
            }
        }
    }

    private double rateMultiplier(Player player, String statId) {
        return modifierResolver == null ? 1.0 : modifierResolver.multiplier(player, statId + "_rate");
    }

    private double effectiveMax(Player player, String statId, double baseMax) {
        return modifierResolver == null ? baseMax : baseMax * modifierResolver.multiplier(player, statId + "_max");
    }

    private void tickThresholds() {

        for (StatDefinition stat : statManager.getAll()) {

            if (!stat.enabled() || stat.thresholds().isEmpty()) {
                continue;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                tickThresholdsFor(player, stat);
            }
        }
    }

    private void tickThresholdsFor(Player player, StatDefinition stat) {

        double value = get(player, stat.id());
        Set<String> active = activeThresholds.computeIfAbsent(player.getUniqueId(), id -> new HashSet<>());

        for (StatThreshold threshold : stat.thresholds()) {

            String key = stat.id() + ":" + threshold.comparison();
            boolean matches = NumericComparison.evaluate(threshold.comparison(), value);

            if (!matches) {
                if (active.remove(key)) {
                    removeThresholdConditions(player, threshold);
                }
                continue;
            }

            applyPotions(player, threshold.potions());

            if (active.add(key)) {
                actionExecutor.execute(player, threshold.actions());
                applyThresholdConditions(player, threshold);
            }
        }
    }

    private void applyThresholdConditions(Player player, StatThreshold threshold) {

        if (conditionManager == null || threshold.applyConditions().isEmpty()) {
            return;
        }

        for (String conditionId : threshold.applyConditions()) {
            conditionManager.get(conditionId).ifPresent(definition -> conditionRuntime.apply(player, definition));
        }
    }

    private void removeThresholdConditions(Player player, StatThreshold threshold) {

        if (conditionRuntime == null) {
            return;
        }

        for (String conditionId : threshold.applyConditions()) {
            conditionRuntime.remove(player, conditionId);
        }
    }

    private void applyPotions(Player player, List<PotionSpec> potions) {

        for (PotionSpec spec : potions) {

            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(spec.type().toLowerCase(Locale.ROOT)));

            if (type == null) {
                continue;
            }

            player.addPotionEffect(new PotionEffect(type, THRESHOLD_CHECK_INTERVAL_TICKS + 5, spec.amplifier(), true, false));
        }
    }

    public double get(Player player, String statId) {

        return values.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                .computeIfAbsent(statId, id -> statManager.get(id).map(StatDefinition::start).orElse(0.0));
    }

    public void set(Player player, String statId, double value) {

        double clamped = statManager.get(statId)
                .map(def -> Math.max(0, Math.min(effectiveMax(player, statId, def.max()), value)))
                .orElse(Math.max(0, value));

        values.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>()).put(statId, clamped);
    }

    public void adjust(Player player, String statId, double delta) {
        set(player, statId, get(player, statId) + delta);
    }

    /** Consumo instantáneo por una acción concreta (sprint/jump/attack/...) — sección 3. */
    public void consume(Player player, String statId, String action) {

        StatDefinition definition = statManager.get(statId).orElse(null);

        if (definition == null) {
            return;
        }

        Double amount = definition.consumption().get(action.toLowerCase(Locale.ROOT));

        if (amount != null) {
            adjust(player, statId, -amount);
        }
    }

    /** Reporta una acción a TODOS los stats que la tengan configurada en su "consumption". */
    public void consumeAll(Player player, String action) {
        for (StatDefinition stat : statManager.getAll()) {
            consume(player, stat.id(), action);
        }
    }

    public void initializePlayer(Player player) {
        for (StatDefinition stat : statManager.getAll()) {
            get(player, stat.id());
        }
    }

    public void clear(Player player) {
        values.remove(player.getUniqueId());
        activeThresholds.remove(player.getUniqueId());
    }

}
