package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Aplica y limpia los modificadores físicos de raza (tamaño, velocidad,
 * vida extra, resistencia a knockback) sobre el Player real.
 * <p>
 * Estos AttributeModifier NO persisten entre reinicios del servidor —
 * deben reaplicarse en cada login (ver PlayerEventListener). Se identifican
 * con una NamespacedKey fija por atributo: reaplicar primero remueve
 * cualquier modificador de raza anterior antes de sumar el nuevo, evitando
 * acumulación si el jugador cambia de raza o reingresa varias veces.
 */
public class RaceAttributeApplier {

    private final NamespacedKey scaleKey;
    private final NamespacedKey speedKey;
    private final NamespacedKey healthKey;
    private final NamespacedKey knockbackKey;

    public RaceAttributeApplier(RPGRoll plugin) {
        this.scaleKey = new NamespacedKey(plugin, "race-scale");
        this.speedKey = new NamespacedKey(plugin, "race-speed");
        this.healthKey = new NamespacedKey(plugin, "race-health");
        this.knockbackKey = new NamespacedKey(plugin, "race-knockback");
    }

    public void apply(Player player, Race race) {

        clear(player);

        RacePhysicalModifiers mods = race.physicalModifiers();

        if (mods == null || !mods.hasAnyModifier()) {
            return;
        }

        applyIfPresent(player, Attribute.GENERIC_SCALE, scaleKey,
                mods.scale() - 1.0, AttributeModifier.Operation.ADD_NUMBER);

        applyIfPresent(player, Attribute.GENERIC_MOVEMENT_SPEED, speedKey,
                mods.movementSpeedPercent(), AttributeModifier.Operation.ADD_SCALAR);

        applyIfPresent(player, Attribute.GENERIC_MAX_HEALTH, healthKey,
                mods.extraHealth(), AttributeModifier.Operation.ADD_NUMBER);

        applyIfPresent(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackKey,
                mods.knockbackResistance(), AttributeModifier.Operation.ADD_NUMBER);
    }

    public void clear(Player player) {
        removeIfPresent(player, Attribute.GENERIC_SCALE, scaleKey);
        removeIfPresent(player, Attribute.GENERIC_MOVEMENT_SPEED, speedKey);
        removeIfPresent(player, Attribute.GENERIC_MAX_HEALTH, healthKey);
        removeIfPresent(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackKey);
    }

    private void applyIfPresent(Player player, Attribute attribute, NamespacedKey key, double amount,
            AttributeModifier.Operation operation) {

        if (amount == 0.0) {
            return;
        }

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.addModifier(new AttributeModifier(key, amount, operation));
    }

    private void removeIfPresent(Player player, Attribute attribute, NamespacedKey key) {

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        Optional<AttributeModifier> existing = instance.getModifiers().stream()
                .filter(mod -> mod.getKey().equals(key))
                .findFirst();

        existing.ifPresent(instance::removeModifier);
    }

}