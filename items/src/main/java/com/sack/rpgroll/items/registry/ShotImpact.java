package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Lo que pasa donde pega un disparo de {@code SHOOT}: daño a la criatura ({@code amount}),
 * explosión ({@code explosion} de potencia, {@code break-blocks}, {@code fire}) y el efecto de
 * RPGRoll-FX {@code hit-effect}. Todo a nombre del tirador: la explosión no le hace daño a él y
 * las protecciones (GriefPrevention, WorldGuard) deciden qué bloques se rompen.
 */
record ShotImpact(double amount, float explosion, boolean breakBlocks, boolean fire, String hitEffect) {

    static ShotImpact from(ItemAction action) {
        return new ShotImpact(Double.parseDouble(action.param("amount", "6")),
                Float.parseFloat(action.param("explosion", "0")),
                Boolean.parseBoolean(action.param("break-blocks", "false")),
                Boolean.parseBoolean(action.param("fire", "false")),
                action.param("hit-effect", ""));
    }

    void apply(Player shooter, Location at, LivingEntity target) {
        if (target != null) {
            target.damage(amount, shooter);
        }
        if (explosion > 0) {
            at.getWorld().createExplosion(at, explosion, fire, breakBlocks, shooter);
        }
        if (!hitEffect.isBlank()) {
            com.sack.rpgroll.common.integration.ParticlesIntegration.play(hitEffect, shooter, at);
        }
    }
}
