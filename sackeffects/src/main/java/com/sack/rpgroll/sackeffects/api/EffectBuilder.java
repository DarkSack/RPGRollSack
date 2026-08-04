package com.sack.rpgroll.sackeffects.api;

import com.sack.rpgroll.sackeffects.core.EffectDefinition;
import com.sack.rpgroll.sackeffects.core.EffectStep;
import com.sack.rpgroll.sackeffects.core.EffectStepType;
import com.sack.rpgroll.sackeffects.core.EffectTarget;
import com.sack.rpgroll.sackeffects.engine.EffectContext;
import com.sack.rpgroll.sackeffects.engine.EffectEngine;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Constructor fluido para un efecto de una sola vez, sin necesitar YAML —
 * pensado para addons que quieren disparar algo puntual desde código:
 *
 * <pre>{@code
 * SackEffectsAPI.get().builder()
 *     .particle(Particle.FLAME).shape("SPHERE").radius(1.5).points(40)
 *     .then().sound(Sound.ENTITY_BLAZE_SHOOT).volume(1).pitch(1.2)
 *     .play(caster);
 * }</pre>
 * <p>
 * Cada método que abre un step ({@code particle}/{@code sound}/{@code
 * title}/{@code actionBar}/{@code bossBar}/{@code potion}) cierra
 * automáticamente el anterior — {@link #then()} solo existe para que la
 * cadena se lea bien, no hace falta llamarlo. Reutiliza el mismo {@link
 * EffectEngine} que ejecuta los efectos definidos en YAML, así que el
 * comportamiento es idéntico.
 */
public final class EffectBuilder {

    private final EffectEngine engine;
    private final List<EffectStep> steps = new ArrayList<>();

    private EffectStepType currentType;
    private int currentDelay = 0;
    private final Map<String, String> currentParams = new LinkedHashMap<>();

    EffectBuilder(EffectEngine engine) {
        this.engine = engine;
    }

    // ============ Iniciar un step ============

    public EffectBuilder particle(Particle particle) {
        return startStep(EffectStepType.PARTICLE).set("particle", particle.name());
    }

    public EffectBuilder sound(Sound sound) {
        return startStep(EffectStepType.SOUND).set("sound", sound.name());
    }

    public EffectBuilder title(String title) {
        return startStep(EffectStepType.TITLE).set("title", title);
    }

    public EffectBuilder actionBar(String text) {
        return startStep(EffectStepType.ACTIONBAR).set("text", text);
    }

    public EffectBuilder bossBar(String title) {
        return startStep(EffectStepType.BOSSBAR).set("title", title);
    }

    public EffectBuilder potion(PotionEffectType type) {
        return startStep(EffectStepType.POTION).set("potion", type.getName());
    }

    /** Puramente cosmético para que la cadena se lea "step, then() step, then() step". */
    public EffectBuilder then() {
        commitCurrentStep();
        return this;
    }

    // ============ Modificadores del step actualmente abierto ============

    public EffectBuilder shape(String shape) {
        return set("shape", shape);
    }

    public EffectBuilder radius(double radius) {
        return set("radius", String.valueOf(radius));
    }

    public EffectBuilder points(int points) {
        return set("points", String.valueOf(points));
    }

    public EffectBuilder height(double height) {
        return set("height", String.valueOf(height));
    }

    public EffectBuilder turns(double turns) {
        return set("turns", String.valueOf(turns));
    }

    public EffectBuilder length(double length) {
        return set("length", String.valueOf(length));
    }

    public EffectBuilder count(int count) {
        return set("count", String.valueOf(count));
    }

    public EffectBuilder speed(double speed) {
        return set("speed", String.valueOf(speed));
    }

    public EffectBuilder volume(double volume) {
        return set("volume", String.valueOf(volume));
    }

    public EffectBuilder pitch(double pitch) {
        return set("pitch", String.valueOf(pitch));
    }

    public EffectBuilder subtitle(String subtitle) {
        return set("subtitle", subtitle);
    }

    public EffectBuilder fadeIn(int ticks) {
        return set("fade-in", String.valueOf(ticks));
    }

    public EffectBuilder stay(int ticks) {
        return set("stay", String.valueOf(ticks));
    }

    public EffectBuilder fadeOut(int ticks) {
        return set("fade-out", String.valueOf(ticks));
    }

    public EffectBuilder duration(int ticks) {
        return set("duration", String.valueOf(ticks));
    }

    public EffectBuilder amplifier(int amplifier) {
        return set("amplifier", String.valueOf(amplifier));
    }

    public EffectBuilder color(String color) {
        return set("color", color);
    }

    public EffectBuilder style(String style) {
        return set("style", style);
    }

    public EffectBuilder target(EffectTarget target) {
        return set("target", target.name());
    }

    public EffectBuilder around(EffectTarget target) {
        return set("around", target.name());
    }

    public EffectBuilder from(EffectTarget target) {
        return set("from", target.name());
    }

    public EffectBuilder to(EffectTarget target) {
        return set("to", target.name());
    }

    public EffectBuilder param(String key, String value) {
        return set(key, value);
    }

    /** Delay del step actual (en ticks, desde que se dispara toda la secuencia — no encadenado). */
    public EffectBuilder delay(int ticks) {
        this.currentDelay = ticks;
        return this;
    }

    // ============ Disparo ============

    public void play(Player caster) {
        commitCurrentStep();
        engine.play(toDefinition(), EffectContext.of(caster));
    }

    public void play(Player caster, Entity target) {
        commitCurrentStep();
        engine.play(toDefinition(), EffectContext.of(caster, target));
    }

    public void play(Player caster, Location targetLocation) {
        commitCurrentStep();
        engine.play(toDefinition(), EffectContext.of(caster, targetLocation));
    }

    private EffectBuilder startStep(EffectStepType type) {
        commitCurrentStep();
        currentType = type;
        return this;
    }

    private EffectBuilder set(String key, String value) {

        if (currentType == null) {
            throw new IllegalStateException(
                    "Llamá a particle()/sound()/title()/actionBar()/bossBar()/potion() antes de configurar sus parámetros.");
        }

        currentParams.put(key, value);
        return this;
    }

    private void commitCurrentStep() {

        if (currentType == null) {
            return;
        }

        steps.add(new EffectStep(currentType, currentDelay, new LinkedHashMap<>(currentParams)));

        currentType = null;
        currentDelay = 0;
        currentParams.clear();
    }

    private EffectDefinition toDefinition() {
        return new EffectDefinition("builder-" + UUID.randomUUID(), "builder", "", List.copyOf(steps));
    }

}
