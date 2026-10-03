package com.sack.rpgroll.enchantments.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Datos disponibles para ejecutar los efectos de un encantamiento:
 * quién lo activó, contra quién (si aplica) y los datos numéricos del
 * nivel actual (para resolver placeholders "{clave}" en los params).
 * <p>
 * {@code item} y {@code slot} son el ítem encantado y dónde lo lleva el
 * jugador; {@code event} es el evento que disparó el trigger. Los efectos de
 * herramienta y de escudo los necesitan (qué bloque, qué golpe); los demás
 * los ignoran. Pueden ser null.
 */
public record EffectContext(Player player, LivingEntity target, int level, Map<String, Double> levelData,
                            ItemStack item, EquipmentSlot slot, Event event) {

    public EffectContext(Player player, LivingEntity target, int level, Map<String, Double> levelData) {
        this(player, target, level, levelData, null, null, null);
    }

    /** El ítem encantado es el de la mano principal (herramientas y armas). */
    public boolean inMainHand() {
        return slot == EquipmentSlot.HAND;
    }

}
