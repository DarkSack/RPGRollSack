package com.sack.rpgroll.magic.engine;

import com.sack.rpgroll.magic.affinity.AffinityResolver;
import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.magic.core.Rune;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.SchoolManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCatalyst;
import com.sack.rpgroll.magic.core.SpellComponent;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;
import com.sack.rpgroll.sackeffects.api.SackEffectsAPI;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Orquesta un cast completo: chequea/descuenta costo, chequea/inicia
 * cooldown, resuelve las runas adjuntas, arma el {@link SpellCastContext} y
 * corre el pipeline de componentes en secuencia — pausando en PROJECTILE
 * (colisión sobre varios ticks) y DELAY (espera), todo lo demás corre
 * sincrónico dentro del mismo tick.
 */
public class SpellCastEngine {

    private final Plugin plugin;
    private final SchoolManager schoolManager;
    private final RuneManager runeManager;
    private final SpellComponentExecutor executor;

    public SpellCastEngine(Plugin plugin, SchoolManager schoolManager, RuneManager runeManager) {
        this.plugin = plugin;
        this.schoolManager = schoolManager;
        this.runeManager = runeManager;
        this.executor = new SpellComponentExecutor(plugin);
    }

    public CastResult cast(Spell spell, Player caster, PlayerSpellbook spellbook, SpellCatalyst catalyst) {

        Optional<MagicSchool> schoolOpt = schoolManager.get(spell.schoolId());

        if (schoolOpt.isEmpty()) {
            return CastResult.failure("La escuela '" + spell.schoolId() + "' no existe.");
        }

        MagicSchool school = schoolOpt.get();

        List<Rune> runes = spellbook.runesFor(spell.id()).stream()
                .map(runeManager::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        var costCooldownMods = RuneModifierApplier.resolveCostCooldown(runes);

        long now = System.currentTimeMillis();

        if (spellbook.isOnCooldown(spell.id(), now)) {
            double secondsLeft = spellbook.remainingCooldownMillis(spell.id(), now) / 1000.0;
            return CastResult.failure(String.format(Locale.ROOT, "En cooldown (%.1fs restantes).", secondsLeft));
        }

        double costMultiplier = (catalyst != null ? catalyst.costMultiplier() : 1.0) * costCooldownMods.costMultiplier();
        List<String> reasons = SpellCostChecker.checkOnly(caster, spell.cost(), costMultiplier);

        if (!reasons.isEmpty()) {
            return CastResult.failure(reasons);
        }

        SpellCostChecker.deduct(caster, spell.cost(), costMultiplier);

        long cooldownMillis = (long) (spell.cooldownTicks() * 50L * costCooldownMods.cooldownMultiplier());
        spellbook.startCooldown(spell.id(), Math.max(0, cooldownMillis), now);

        double affinityMultiplier = AffinityResolver.multiplierFor(caster, school);
        double powerMultiplier = affinityMultiplier * (catalyst != null ? catalyst.powerMultiplier() : 1.0);
        double rangeMultiplier = catalyst != null ? catalyst.rangeMultiplier() : 1.0;

        SpellCastContext context = new SpellCastContext(caster, spell, school, catalyst, powerMultiplier,
                rangeMultiplier);

        RuneModifierApplier.applyToContext(context, runes);

        playCastFeedback(school, caster);

        runFrom(context, spell.components(), 0);

        return CastResult.ok();
    }

    private void playCastFeedback(MagicSchool school, Player caster) {

        if (school.castSoundOnCast() != null) {
            try {
                Sound sound = Sound.valueOf(school.castSoundOnCast().trim().toUpperCase(Locale.ROOT));
                caster.getWorld().playSound(caster.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (school.castEffectId() != null && SackEffectsAPI.isReady()) {
            SackEffectsAPI.get().play(school.castEffectId(), caster);
        }
    }

    private void runFrom(SpellCastContext context, List<SpellComponent> components, int index) {

        if (index >= components.size()) {
            return;
        }

        SpellComponent component = components.get(index);

        switch (component.type()) {

            case PROJECTILE -> executeProjectile(component, context, components, index);

            case DELAY -> {
                int ticks = Math.max(1, component.paramInt("ticks", 20));
                Bukkit.getScheduler().runTaskLater(plugin, () -> runFrom(context, components, index + 1), ticks);
            }

            default -> {
                executor.execute(component, context);
                runFrom(context, components, index + 1);
            }
        }
    }

    private void executeProjectile(SpellComponent component, SpellCastContext context, List<SpellComponent> components,
            int index) {

        int extras = context.extraProjectiles();
        double spreadDegrees = component.paramDouble("spread-degrees", 15);

        launchProjectile(component, context, 0, components, index);

        for (int i = 1; i <= extras; i++) {

            SpellCastContext clone = context.copyForExtraProjectile();
            double sign = (i % 2 == 0) ? -1 : 1;
            double angle = spreadDegrees * Math.ceil(i / 2.0) * sign;

            launchProjectile(component, clone, angle, components, index);
        }
    }

    private void launchProjectile(SpellComponent component, SpellCastContext context, double yawOffsetDegrees,
            List<SpellComponent> components, int index) {

        Player caster = context.caster();
        World world = caster.getWorld();
        Location origin = caster.getEyeLocation().clone();
        Vector direction = yawOffsetDegrees == 0 ? origin.getDirection().clone()
                : rotateAroundY(origin.getDirection(), yawOffsetDegrees);

        double speed = Math.max(0.1, component.paramDouble("speed", 0.8));
        double maxDistance = component.paramDouble("max-distance", 20) * context.rangeMultiplier();
        double hitRadius = component.paramDouble("hit-radius", 1.0);
        Particle trailParticle = parseParticle(component.param("trail-particle", "ENCHANTED_HIT"));
        int maxTicks = (int) Math.ceil(maxDistance / speed) + 5;

        context.setCurrentLocation(origin.clone());

        new BukkitRunnable() {

            int traveled = 0;
            final Location current = origin.clone();

            @Override
            public void run() {

                if (traveled >= maxTicks) {
                    cancel();
                    runFrom(context, components, index + 1);
                    return;
                }

                current.add(direction.clone().multiply(speed));
                context.setCurrentLocation(current.clone());
                world.spawnParticle(trailParticle, current, 1, 0, 0, 0, 0);

                if (current.getBlock().getType().isSolid()) {
                    cancel();
                    runFrom(context, components, index + 1);
                    return;
                }

                for (Entity entity : world.getNearbyEntities(current, hitRadius, hitRadius, hitRadius)) {

                    if (!(entity instanceof LivingEntity living) || entity.equals(caster)
                            || context.alreadyHit().contains(entity.getUniqueId())) {
                        continue;
                    }

                    context.alreadyHit().add(entity.getUniqueId());

                    List<LivingEntity> targets = new ArrayList<>(context.currentTargets());
                    targets.add(living);
                    context.setCurrentTargets(targets);

                    if (!context.piercing() || targets.size() >= context.maxPierces()) {
                        cancel();
                        runFrom(context, components, index + 1);
                        return;
                    }
                }

                traveled++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Vector rotateAroundY(Vector vector, double degrees) {

        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double x = vector.getX() * cos + vector.getZ() * sin;
        double z = -vector.getX() * sin + vector.getZ() * cos;

        return new Vector(x, vector.getY(), z).normalize();
    }

    private Particle parseParticle(String raw) {
        try {
            return Particle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Particle.ENCHANTED_HIT;
        }
    }

}
