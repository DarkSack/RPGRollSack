package com.sack.rpgroll.extras.temperature;

import com.sack.rpgroll.extras.action.ExtrasActionExecutor;
import com.sack.rpgroll.extras.stat.PotionSpec;
import com.sack.rpgroll.extras.thermal.ThermalProperties;
import com.sack.rpgroll.extras.thermal.ThermalProtectionService;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temperatura corporal (sección 7): converge gradualmente hacia la
 * ambiental, amortiguada por la protección térmica de la armadura
 * equipada. Corre en un único scheduler al intervalo configurado — nunca
 * por tick — y mapea el resultado a un {@link TemperatureStateRange} con
 * sus propios efectos.
 */
public class BodyTemperatureEngine {

    private static final double DEFAULT_START = 36.7;

    private final Plugin plugin;
    private final AmbientTemperatureCalculator ambientCalculator;
    private final ThermalProtectionService thermalProtectionService;
    private final ExtrasActionExecutor actionExecutor;
    private final TemperatureSettings settings;

    private final Map<UUID, Double> bodyTemperature = new ConcurrentHashMap<>();
    private final Map<UUID, Double> ambientTemperature = new ConcurrentHashMap<>();
    private final Map<UUID, String> currentState = new ConcurrentHashMap<>();

    private BukkitTask task;

    public BodyTemperatureEngine(Plugin plugin, AmbientTemperatureCalculator ambientCalculator,
            ThermalProtectionService thermalProtectionService, ExtrasActionExecutor actionExecutor,
            TemperatureSettings settings) {
        this.plugin = plugin;
        this.ambientCalculator = ambientCalculator;
        this.thermalProtectionService = thermalProtectionService;
        this.actionExecutor = actionExecutor;
        this.settings = settings;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick,
                settings.updateIntervalTicks(), settings.updateIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayer(player);
        }
    }

    private void tickPlayer(Player player) {

        double ambient = ambientCalculator.calculate(player);
        ambientTemperature.put(player.getUniqueId(), ambient);

        double current = bodyTemperature.computeIfAbsent(player.getUniqueId(), id -> DEFAULT_START);

        ThermalProperties protection = thermalProtectionService.netProtection(player);
        double dampening = current < ambient ? protection.coldResistance() : protection.heatResistance();
        double effectiveRate = settings.exchangeRate() * (1 - Math.max(protection.insulation(), dampening));

        double next = current + (ambient - current) * effectiveRate;
        bodyTemperature.put(player.getUniqueId(), next);

        applyState(player, next);
    }

    private void applyState(Player player, double bodyTemp) {

        TemperatureStateRange matched = settings.states().stream()
                .filter(range -> range.matches(bodyTemp))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            return;
        }

        applyPotions(player, matched.potions());

        String previous = currentState.put(player.getUniqueId(), matched.id());

        if (!matched.id().equals(previous)) {
            actionExecutor.execute(player, matched.actions());
        }
    }

    private void applyPotions(Player player, java.util.List<PotionSpec> potions) {

        for (PotionSpec spec : potions) {

            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(spec.type().toLowerCase(Locale.ROOT)));

            if (type == null) {
                continue;
            }

            player.addPotionEffect(new PotionEffect(type, settings.updateIntervalTicks() + 10, spec.amplifier(), true, false));
        }
    }

    public double bodyTemperature(Player player) {
        return bodyTemperature.computeIfAbsent(player.getUniqueId(), id -> DEFAULT_START);
    }

    public double ambientTemperature(Player player) {
        return ambientTemperature.getOrDefault(player.getUniqueId(), DEFAULT_START);
    }

    public String currentStateId(Player player) {
        return currentState.getOrDefault(player.getUniqueId(), "normal");
    }

    public void clear(Player player) {
        bodyTemperature.remove(player.getUniqueId());
        ambientTemperature.remove(player.getUniqueId());
        currentState.remove(player.getUniqueId());
    }

}
