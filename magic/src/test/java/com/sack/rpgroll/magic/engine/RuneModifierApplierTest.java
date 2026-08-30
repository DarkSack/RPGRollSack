package com.sack.rpgroll.magic.engine;

import com.sack.rpgroll.magic.core.Rune;
import com.sack.rpgroll.magic.core.RuneModifierType;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.magic.core.SpellCatalyst;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuneModifierApplierTest {

    private Rune costRune(double multiplier) {
        return new Rune("cost-" + multiplier, "Cost", null, null, RuneModifierType.COST_MODIFIER,
                Map.of("multiplier", String.valueOf(multiplier)));
    }

    private Rune cooldownRune(double multiplier) {
        return new Rune("cooldown-" + multiplier, "Cooldown", null, null, RuneModifierType.COOLDOWN_MODIFIER,
                Map.of("multiplier", String.valueOf(multiplier)));
    }

    @Test
    void resolveCostCooldownDefaultsToOneWithNoRunes() {
        var result = RuneModifierApplier.resolveCostCooldown(List.of());

        assertEquals(1.0, result.costMultiplier());
        assertEquals(1.0, result.cooldownMultiplier());
    }

    @Test
    void resolveCostCooldownMultipliesCostAcrossMultipleCostRunes() {
        var result = RuneModifierApplier.resolveCostCooldown(List.of(costRune(0.5), costRune(0.5)));

        assertEquals(0.25, result.costMultiplier(), 0.0001);
    }

    @Test
    void resolveCostCooldownMultipliesCooldownIndependentlyFromCost() {
        var result = RuneModifierApplier.resolveCostCooldown(List.of(costRune(2.0), cooldownRune(0.5)));

        assertEquals(2.0, result.costMultiplier());
        assertEquals(0.5, result.cooldownMultiplier());
    }

    @Test
    void resolveCostCooldownIgnoresNonModifierRunes() {
        Rune piercing = new Rune("piercing", "Piercing", null, null, RuneModifierType.PIERCING, Map.of());

        var result = RuneModifierApplier.resolveCostCooldown(List.of(piercing));

        assertEquals(1.0, result.costMultiplier());
        assertEquals(1.0, result.cooldownMultiplier());
    }

    @Test
    void resolveCostCooldownNeverGoesNegative() {
        var result = RuneModifierApplier.resolveCostCooldown(List.of(costRune(-5.0)));

        assertEquals(0.0, result.costMultiplier());
    }

    private SpellCastContext newContext() {
        Player player = mock(Player.class);
        Location eyeLocation = new Location(null, 0, 64, 0);
        when(player.getEyeLocation()).thenReturn(eyeLocation);

        Spell spell = mock(Spell.class);
        MagicSchool school = mock(MagicSchool.class);
        SpellCatalyst catalyst = mock(SpellCatalyst.class);

        return new SpellCastContext(player, spell, school, catalyst, 1.0, 1.0);
    }

    @Test
    void applyToContextAccumulatesExtraProjectilesAcrossMultipleRunes() {
        SpellCastContext context = newContext();
        Rune runeA = new Rune("a", "A", null, null, RuneModifierType.EXTRA_PROJECTILES, Map.of("count", "2"));
        Rune runeB = new Rune("b", "B", null, null, RuneModifierType.EXTRA_PROJECTILES, Map.of("count", "3"));

        RuneModifierApplier.applyToContext(context, List.of(runeA, runeB));

        assertEquals(5, context.extraProjectiles());
    }

    @Test
    void applyToContextPiercingTakesTheHigherMaxPierces() {
        SpellCastContext context = newContext();
        Rune weak = new Rune("weak", "Weak", null, null, RuneModifierType.PIERCING, Map.of("max-pierces", "2"));
        Rune strong = new Rune("strong", "Strong", null, null, RuneModifierType.PIERCING, Map.of("max-pierces", "5"));

        RuneModifierApplier.applyToContext(context, List.of(weak, strong));

        assertTrue(context.piercing());
        assertEquals(5, context.maxPierces());
    }

    @Test
    void applyToContextExplosiveTakesTheHigherRadiusAndDamageIndependently() {
        SpellCastContext context = newContext();
        Rune bigRadius = new Rune("r", "R", null, null, RuneModifierType.EXPLOSIVE,
                Map.of("radius", "6", "damage", "1"));
        Rune bigDamage = new Rune("d", "D", null, null, RuneModifierType.EXPLOSIVE,
                Map.of("radius", "1", "damage", "9"));

        RuneModifierApplier.applyToContext(context, List.of(bigRadius, bigDamage));

        assertEquals(6.0, context.explosiveRadius());
        assertEquals(9.0, context.explosiveDamage());
    }

    @Test
    void applyToContextSetsExtraEffectIdFromApplyEffectRune() {
        SpellCastContext context = newContext();
        Rune effectRune = new Rune("e", "E", null, null, RuneModifierType.APPLY_EFFECT,
                Map.of("effect-id", "burn"));

        RuneModifierApplier.applyToContext(context, List.of(effectRune));

        assertEquals("burn", context.extraEffectId());
    }

    @Test
    void applyToContextIgnoresCostAndCooldownModifierRunes() {
        SpellCastContext context = newContext();

        RuneModifierApplier.applyToContext(context, List.of(costRune(0.5), cooldownRune(0.5)));

        assertFalse(context.piercing());
        assertEquals(0, context.extraProjectiles());
    }
}
