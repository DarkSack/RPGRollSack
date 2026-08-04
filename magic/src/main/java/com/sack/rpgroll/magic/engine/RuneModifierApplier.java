package com.sack.rpgroll.magic.engine;

import com.sack.rpgroll.magic.core.Rune;

import java.util.List;

/**
 * Traduce las runas adjuntas a un hechizo (ver {@code PlayerSpellbook#runesFor})
 * en modificadores concretos — nunca toca la definición guardada del
 * hechizo, solo un {@link SpellCastContext} (o los multiplicadores de
 * costo/cooldown, resueltos antes de construir el contexto).
 */
public final class RuneModifierApplier {

    private RuneModifierApplier() {
    }

    public record CostCooldownModifiers(double costMultiplier, double cooldownMultiplier) {
    }

    public static CostCooldownModifiers resolveCostCooldown(List<Rune> runes) {

        double cost = 1.0;
        double cooldown = 1.0;

        for (Rune rune : runes) {
            switch (rune.type()) {
                case COST_MODIFIER -> cost *= rune.paramDouble("multiplier", 1.0);
                case COOLDOWN_MODIFIER -> cooldown *= rune.paramDouble("multiplier", 1.0);
                default -> {
                }
            }
        }

        return new CostCooldownModifiers(Math.max(0, cost), Math.max(0, cooldown));
    }

    public static void applyToContext(SpellCastContext context, List<Rune> runes) {

        for (Rune rune : runes) {
            switch (rune.type()) {

                case EXTRA_PROJECTILES ->
                        context.setExtraProjectiles(context.extraProjectiles() + rune.paramInt("count", 1));

                case PIERCING -> context.setPiercing(Math.max(context.maxPierces(), rune.paramInt("max-pierces", 3)));

                case EXPLOSIVE -> context.setExplosive(
                        Math.max(context.explosiveRadius(), rune.paramDouble("radius", 3)),
                        Math.max(context.explosiveDamage(), rune.paramDouble("damage", 4)));

                case APPLY_EFFECT -> context.setExtraEffectId(rune.param("effect-id", null));

                case COST_MODIFIER, COOLDOWN_MODIFIER -> {
                    // Ya resueltos en resolveCostCooldown() antes de llegar acá.
                }
            }
        }
    }

}
