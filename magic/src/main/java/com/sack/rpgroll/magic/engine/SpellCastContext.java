package com.sack.rpgroll.magic.engine;

import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCatalyst;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Estado mutable de UN cast en curso — se pasa de componente en componente
 * a medida que {@code SpellCastEngine} avanza el pipeline. {@code
 * currentLocation}/{@code currentTargets} son lo que un componente deja
 * para que el siguiente lo use (ej. PROJECTILE deja el punto de colisión y
 * las entidades golpeadas, que DAMAGE_DIRECT/HEAL/APPLY_EFFECT/PUSH/PULL
 * leen después).
 * <p>
 * Los campos {@code extraProjectiles}/{@code piercing}/{@code
 * explosiveRadius}/{@code extraEffectId} los rellena {@code
 * RuneModifierApplier} antes de ejecutar el pipeline — nunca se leen desde
 * el YAML del hechizo en sí.
 */
public final class SpellCastContext {

    private final Player caster;
    private final Spell spell;
    private final MagicSchool school;
    private final SpellCatalyst catalyst;
    private final double powerMultiplier;
    private final double rangeMultiplier;

    private Location currentLocation;
    private List<LivingEntity> currentTargets = new ArrayList<>();
    private final Set<UUID> alreadyHit = new HashSet<>();

    private int extraProjectiles;
    private boolean piercing;
    private int maxPierces = 1;
    private double explosiveRadius;
    private double explosiveDamage;
    private boolean explosiveConsumed;
    private String extraEffectId;

    public SpellCastContext(Player caster, Spell spell, MagicSchool school, SpellCatalyst catalyst,
            double powerMultiplier, double rangeMultiplier) {
        this.caster = caster;
        this.spell = spell;
        this.school = school;
        this.catalyst = catalyst;
        this.powerMultiplier = powerMultiplier;
        this.rangeMultiplier = rangeMultiplier;
        this.currentLocation = caster.getEyeLocation();
    }

    /** Copia superficial para EXTRA_PROJECTILES — cada proyectil extra corre el resto del pipeline de forma independiente. */
    public SpellCastContext copyForExtraProjectile() {

        SpellCastContext copy = new SpellCastContext(caster, spell, school, catalyst, powerMultiplier,
                rangeMultiplier);
        copy.currentLocation = currentLocation.clone();
        copy.piercing = piercing;
        copy.maxPierces = maxPierces;
        copy.explosiveRadius = explosiveRadius;
        copy.explosiveDamage = explosiveDamage;
        copy.extraEffectId = extraEffectId;
        // extraProjectiles se deja en 0 en la copia — si no, cada clon volvería a clonarse infinitamente.
        return copy;
    }

    public Player caster() {
        return caster;
    }

    public Spell spell() {
        return spell;
    }

    public MagicSchool school() {
        return school;
    }

    public SpellCatalyst catalyst() {
        return catalyst;
    }

    public double powerMultiplier() {
        return powerMultiplier;
    }

    public double rangeMultiplier() {
        return rangeMultiplier;
    }

    public Location currentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }

    public List<LivingEntity> currentTargets() {
        return currentTargets;
    }

    public void setCurrentTargets(List<LivingEntity> targets) {
        this.currentTargets = targets;
    }

    public Set<UUID> alreadyHit() {
        return alreadyHit;
    }

    public int extraProjectiles() {
        return extraProjectiles;
    }

    public void setExtraProjectiles(int extraProjectiles) {
        this.extraProjectiles = Math.max(0, extraProjectiles);
    }

    public boolean piercing() {
        return piercing;
    }

    public int maxPierces() {
        return maxPierces;
    }

    public void setPiercing(int maxPierces) {
        this.piercing = true;
        this.maxPierces = Math.max(1, maxPierces);
    }

    public double explosiveRadius() {
        return explosiveRadius;
    }

    public double explosiveDamage() {
        return explosiveDamage;
    }

    public void setExplosive(double radius, double damage) {
        this.explosiveRadius = radius;
        this.explosiveDamage = damage;
    }

    public String extraEffectId() {
        return extraEffectId;
    }

    public void setExtraEffectId(String extraEffectId) {
        this.extraEffectId = extraEffectId;
    }

    public boolean explosiveConsumed() {
        return explosiveConsumed;
    }

    public void markExplosiveConsumed() {
        this.explosiveConsumed = true;
    }

}
