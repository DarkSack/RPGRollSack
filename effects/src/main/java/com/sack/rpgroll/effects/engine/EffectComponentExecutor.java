package com.sack.rpgroll.effects.engine;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.effects.core.EffectComponent;
import com.sack.rpgroll.effects.core.EffectComponentType;
import com.sack.rpgroll.effects.core.EffectTriggerType;
import com.sack.rpgroll.effects.runtime.ActiveEffect;
import com.sack.rpgroll.sackeffects.api.SackEffectsAPI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Optional;

/**
 * Ejecuta un {@link EffectComponent} contra la entidad que tiene el efecto
 * activo. Cada tipo de componente es independiente — un error en uno (ej.
 * atributo/sonido inválido en el YAML) solo loguea un warning y no afecta a
 * los demás componentes ni al resto del efecto.
 * <p>
 * VISUAL/SOUND delegan en {@code SackEffectsAPI} (dependencia blanda — si
 * RPGRoll-Particles no está instalado, esos componentes simplemente no hacen
 * nada) y solo tienen efecto sobre {@link Player}, ya que esa API solo
 * acepta un Player como "caster".
 */
public class EffectComponentExecutor {


    private final Plugin plugin;

    public EffectComponentExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    public void fireTrigger(LivingEntity target, ActiveEffect activeEffect, EffectTriggerType trigger) {

        for (EffectComponent component : activeEffect.definition().components()) {

            boolean matches = component.trigger() == trigger || (isLifecycleBound(component.type())
                    && (trigger == EffectTriggerType.ON_APPLY || trigger == EffectTriggerType.ON_EXPIRE
                            || trigger == EffectTriggerType.ON_REMOVE));

            if (!matches) {
                continue;
            }

            try {
                execute(target, activeEffect, component, trigger);
            } catch (Throwable e) {
                // Throwable (no solo Exception) a propósito: VISUAL/AURA dependen de otros
                // plugins vía softdepend (RPGRoll-Particles) — si no están instalados, resolver esa
                // clase lanza NoClassDefFoundError, no una Exception normal.
                plugin.getLogger().warning("✘ Error ejecutando componente " + component.type() + " del efecto '"
                        + activeEffect.definition().id() + "': " + e.getMessage());
            }
        }
    }

    /**
     * ATTRIBUTE_MODIFIER/MOVEMENT_MODIFIER necesitan correr tanto al aplicar
     * (agregar el modifier) como al expirar/remover (sacarlo) — por eso
     * ignoran el {@code trigger} que el YAML les haya puesto y siempre
     * disparan en los 3 momentos del ciclo de vida del efecto.
     */
    private boolean isLifecycleBound(EffectComponentType type) {
        return type == EffectComponentType.ATTRIBUTE_MODIFIER || type == EffectComponentType.MOVEMENT_MODIFIER;
    }

    private void execute(LivingEntity target, ActiveEffect activeEffect, EffectComponent component,
            EffectTriggerType trigger) {

        switch (component.type()) {
            case ATTRIBUTE_MODIFIER -> executeAttributeModifier(target, activeEffect, component, trigger);
            case MOVEMENT_MODIFIER -> executeMovementModifier(target, activeEffect, component, trigger);
            case PERIODIC_DAMAGE -> executePeriodicDamage(target, activeEffect, component);
            case PERIODIC_HEAL -> executePeriodicHeal(target, activeEffect, component);
            case POTION_VANILLA -> executePotionVanilla(target, component);
            case VISUAL -> executeVisual(target, component);
            case SOUND -> executeSound(target, component);
            case MESSAGE -> executeMessage(target, component);
            case SHIELD -> executeShield(target, component);
            case SILENCE, CONFUSION -> {
                // Pura bandera — otros addons consultan EffectsAPI#isSilenced/isConfused.
                // No hay nada que ejecutar acá.
            }
            case AURA -> executeAura(target, activeEffect, component);
            case COMMAND -> executeCommand(target, component);
        }
    }

    // ============ ATTRIBUTE_MODIFIER / MOVEMENT_MODIFIER ============

    private void executeAttributeModifier(LivingEntity target, ActiveEffect activeEffect, EffectComponent component,
            EffectTriggerType trigger) {

        Attribute attribute = parseAttribute(component.param("attribute", "MOVEMENT_SPEED"));
        if (attribute == null) {
            return;
        }

        applyOrRemoveAttribute(target, activeEffect, component, attribute, trigger);
    }

    private void executeMovementModifier(LivingEntity target, ActiveEffect activeEffect, EffectComponent component,
            EffectTriggerType trigger) {
        applyOrRemoveAttribute(target, activeEffect, component, Attribute.MOVEMENT_SPEED, trigger);
    }

    private void applyOrRemoveAttribute(LivingEntity target, ActiveEffect activeEffect, EffectComponent component,
            Attribute attribute, EffectTriggerType trigger) {

        AttributeInstance instance = target.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        NamespacedKey key = attributeKey(activeEffect, attribute);

        if (trigger == EffectTriggerType.ON_EXPIRE || trigger == EffectTriggerType.ON_REMOVE) {
            Optional<AttributeModifier> existing = instance.getModifiers().stream()
                    .filter(mod -> mod.getKey().equals(key))
                    .findFirst();
            existing.ifPresent(instance::removeModifier);
            return;
        }

        double amount = component.paramDouble("amount", 0);
        if (amount == 0) {
            return;
        }

        AttributeModifier.Operation operation = parseOperation(component.param("operation", "ADD_NUMBER"));
        instance.addModifier(new AttributeModifier(key, amount, operation));
    }

    private NamespacedKey attributeKey(ActiveEffect activeEffect, Attribute attribute) {
        return new NamespacedKey(plugin, "effect-" + activeEffect.definition().id() + "-"
                + attribute.getKey().getKey());
    }

    private Attribute parseAttribute(String raw) {
        try {
            return Attribute.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Atributo inválido: " + raw);
            return null;
        }
    }

    private AttributeModifier.Operation parseOperation(String raw) {
        try {
            return AttributeModifier.Operation.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AttributeModifier.Operation.ADD_NUMBER;
        }
    }

    // ============ PERIODIC_DAMAGE / PERIODIC_HEAL ============

    private void executePeriodicDamage(LivingEntity target, ActiveEffect activeEffect, EffectComponent component) {

        double base = component.paramDouble("amount", 1.0);
        double perStack = component.paramDouble("amount-per-stack", 0.0);
        double amount = base + perStack * (activeEffect.stacks() - 1);

        target.damage(Math.max(0, amount));
    }

    private void executePeriodicHeal(LivingEntity target, ActiveEffect activeEffect, EffectComponent component) {

        double base = component.paramDouble("amount", 1.0);
        double perStack = component.paramDouble("amount-per-stack", 0.0);
        double amount = base + perStack * (activeEffect.stacks() - 1);

        AttributeInstance maxHealthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : target.getHealth();

        target.setHealth(Math.min(maxHealth, target.getHealth() + Math.max(0, amount)));
    }

    // ============ POTION_VANILLA ============

    private void executePotionVanilla(LivingEntity target, EffectComponent component) {

        PotionEffectType type = PotionEffectType.getByName(component.param("potion", "SPEED").toUpperCase(Locale.ROOT));

        if (type == null) {
            plugin.getLogger().warning("✘ Efecto de poción inválido: " + component.param("potion", ""));
            return;
        }

        int durationTicks = component.paramInt("duration", 100);
        int amplifier = component.paramInt("amplifier", 0);

        target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, true));
    }

    // ============ VISUAL / SOUND (delega en RPGRoll-Particles) ============

    private void executeVisual(LivingEntity target, EffectComponent component) {

        if (!(target instanceof Player player) || !SackEffectsAPI.isReady()) {
            return;
        }

        String sackEffectId = component.param("effect", null);

        if (sackEffectId != null) {
            SackEffectsAPI.get().play(sackEffectId, player);
        }
    }

    private void executeSound(LivingEntity target, EffectComponent component) {

        if (!(target instanceof Player player)) {
            return;
        }

        try {
            var sound = org.bukkit.Sound.valueOf(component.param("sound", "ENTITY_PLAYER_HURT").toUpperCase(Locale.ROOT));
            float volume = (float) component.paramDouble("volume", 1.0);
            float pitch = (float) component.paramDouble("pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Sonido inválido: " + component.param("sound", ""));
        }
    }

    // ============ MESSAGE ============

    private void executeMessage(LivingEntity target, EffectComponent component) {

        if (!(target instanceof Player player)) {
            return;
        }

        String raw = component.param("text", "");
        if (raw.isBlank()) {
            return;
        }

        Component text = ComponentUtils.parse(raw);

        if (component.paramBoolean("actionbar", false)) {
            player.sendActionBar(text);
        } else {
            player.sendMessage(text);
        }
    }

    // ============ SHIELD ============

    private void executeShield(LivingEntity target, EffectComponent component) {
        double amount = component.paramDouble("amount", 4.0);
        target.setAbsorptionAmount(target.getAbsorptionAmount() + amount);
    }

    // ============ AURA ============

    private void executeAura(LivingEntity target, ActiveEffect activeEffect, EffectComponent component) {

        String auraEffectId = component.param("effect", activeEffect.definition().id());
        double radius = component.paramDouble("radius", 5.0);

        if (!com.sack.rpgroll.effects.api.EffectsAPI.isReady() || target.getWorld() == null) {
            return;
        }

        for (org.bukkit.entity.Entity nearby : target.getNearbyEntities(radius, radius, radius)) {

            if (!(nearby instanceof LivingEntity living) || living.equals(target)) {
                continue;
            }

            com.sack.rpgroll.effects.api.EffectsAPI.get().apply(auraEffectId, living, target);
        }
    }

    // ============ COMMAND ============

    private void executeCommand(LivingEntity target, EffectComponent component) {

        String template = component.param("command", "");
        if (template.isBlank()) {
            return;
        }

        String command = template.replace("%target%", target.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

}
