package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.Vector;

/** Boca del arma en bloques a escala 1, relativa a la mira. */
record ShotMuzzle(double forward, double right, double down) {
    static ShotMuzzle from(ItemAction action) {
        return new ShotMuzzle(offset(action, "muzzle-forward", 1),
                offset(action, "muzzle-right", .32), offset(action, "muzzle-down", .22));
    }

    private static double offset(ItemAction action, String key, double fallback) {
        double value = Double.parseDouble(action.param(key, Double.toString(fallback)));
        if (!Double.isFinite(value) || Math.abs(value) > 4) {
            throw new IllegalArgumentException(key + " debe ser finito y estar entre -4 y 4");
        }
        return value;
    }

    Location at(Player player, EquipmentSlot hand, Location eye, Vector direction) {
        boolean left = (player.getMainHand() == MainHand.LEFT) ^ (hand == EquipmentSlot.OFF_HAND);
        return at(playerScale(player), left, eye, direction);
    }

    static double playerScale(Player player) {
        var attribute = player.getAttribute(Attribute.SCALE);
        return attribute == null ? 1 : attribute.getValue();
    }

    Location at(double scale, boolean left, Location eye, Vector direction) {
        // La base horizontal conserva su signo incluso mirando verticalmente.
        double yaw = Math.toRadians(eye.getYaw());
        Vector lateral = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vector downAxis = lateral.getCrossProduct(direction).normalize().multiply(-1);
        return eye.clone().add(direction.clone().multiply(forward * scale))
                .add(lateral.multiply(right * scale * (left ? -1 : 1)))
                .add(downAxis.multiply(down * scale));
    }
}
