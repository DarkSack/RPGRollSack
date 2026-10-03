package com.sack.rpgroll.enchantments.effect;

import io.papermc.paper.event.player.PlayerShieldDisableEvent;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Efectos de equipo: reparación con experiencia, daño extra y los del
 * escudo (empujón, reflejo de proyectiles y temple contra hachas).
 */
public class GearEffects {

    // ------------------------------------------------------------------ reparación avanzada

    /**
     * Repara con la experiencia que acaba de recoger el jugador, a
     * {@code ratio} de durabilidad por punto (Reparación vanilla da 2), y
     * gasta solo los puntos usados. Con {@code equipment} reparte por todo el
     * equipo puesto en vez de solo el ítem encantado.
     */
    public void repair(EffectContext context, double ratio, boolean equipment) {

        if (!(context.event() instanceof PlayerExpChangeEvent event) || ratio <= 0) {
            return;
        }

        int experience = event.getAmount();

        if (experience <= 0) {
            return;
        }

        List<ItemStack> targets = equipment ? equipped(context.player()) : List.of(context.item());

        for (ItemStack item : targets) {

            if (experience <= 0) {
                break;
            }

            if (item == null || !(item.getItemMeta() instanceof Damageable meta)
                    || meta.isUnbreakable() || meta.getDamage() <= 0) {
                continue;
            }

            int fixed = Math.min(meta.getDamage(), (int) Math.floor(experience * ratio));

            if (fixed <= 0) {
                continue;
            }

            meta.setDamage(meta.getDamage() - fixed);
            item.setItemMeta(meta);
            experience -= (int) Math.ceil(fixed / ratio);
        }

        event.setAmount(Math.max(0, experience));
    }

    private static List<ItemStack> equipped(Player player) {

        PlayerInventory inventory = player.getInventory();
        List<ItemStack> items = new ArrayList<>();

        items.add(inventory.getItemInMainHand());
        items.add(inventory.getItemInOffHand());
        items.add(inventory.getHelmet());
        items.add(inventory.getChestplate());
        items.add(inventory.getLeggings());
        items.add(inventory.getBoots());

        return items;
    }

    // ------------------------------------------------------------------ filo avanzado

    /**
     * Suma daño al golpe. El extra se escala por la carga del ataque (el daño
     * del golpe frente al ataque completo), igual que el Filo vanilla: dar
     * clics seguidos no regala el bono entero.
     */
    public void damageBonus(EffectContext context, double amount, double multiplier) {

        if (!(context.event() instanceof EntityDamageByEntityEvent event) || !context.inMainHand()) {
            return;
        }

        var attribute = context.player().getAttribute(Attribute.ATTACK_DAMAGE);
        double full = attribute != null ? attribute.getValue() : 0;
        double charge = full > 0 ? Math.max(0, Math.min(1, event.getDamage() / full)) : 1;

        event.setDamage(event.getDamage() * multiplier + amount * charge);
    }

    // ------------------------------------------------------------------ escudo

    /** Empuja al objetivo (el atacante, en los triggers de escudo) lejos del jugador. */
    public void knockback(EffectContext context, double strength, double vertical) {

        LivingEntity target = context.target();

        if (target == null || target.equals(context.player())) {
            return;
        }

        Vector push = target.getLocation().toVector().subtract(context.player().getLocation().toVector()).setY(0);

        if (push.lengthSquared() < 1.0E-4) {
            push = context.player().getLocation().getDirection().setY(0);
        }

        push.normalize().multiply(strength).setY(vertical);
        target.setVelocity(target.getVelocity().add(push));
    }

    /**
     * Devuelve a quien lo disparó el proyectil que paró el escudo, con el
     * mismo daño. {@code chance} en %, para que cada nivel refleje más a menudo.
     */
    public void reflect(EffectContext context, double speed, double chance) {

        if (!(context.event() instanceof EntityDamageByEntityEvent event)
                || !(event.getDamager() instanceof Projectile projectile)
                || !(projectile.getShooter() instanceof LivingEntity shooter)
                || shooter.equals(context.player())
                || ThreadLocalRandom.current().nextDouble(100) >= chance) {
            return;
        }

        Class<? extends Projectile> type = reflectable(projectile);

        if (type == null) {
            return;
        }

        Player player = context.player();
        Vector direction = shooter.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector())
                .normalize().multiply(speed);

        Projectile back = player.launchProjectile(type, direction);

        if (back instanceof AbstractArrow arrow && projectile instanceof AbstractArrow original) {
            arrow.setDamage(original.getDamage());
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            if (arrow instanceof Arrow tipped && original instanceof Arrow source) {
                tipped.setBasePotionType(source.getBasePotionType());
            }
        }

        projectile.remove();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.6f);
    }

    /** Las clases con las que se puede volver a lanzar; tridentes y similares no (se duplicarían). */
    private static Class<? extends Projectile> reflectable(Projectile projectile) {

        if (projectile instanceof SpectralArrow) {
            return SpectralArrow.class;
        }
        if (projectile instanceof Arrow) {
            return Arrow.class;
        }
        if (projectile instanceof SmallFireball) {
            return SmallFireball.class;
        }
        if (projectile instanceof LargeFireball) {
            return LargeFireball.class;
        }
        if (projectile instanceof WitherSkull) {
            return WitherSkull.class;
        }

        return null;
    }

    /** Multiplica el tiempo que un hacha deja el escudo desactivado; 0 lo evita del todo. */
    public void shieldCooldown(EffectContext context, double factor) {

        if (!(context.event() instanceof PlayerShieldDisableEvent event)) {
            return;
        }

        if (factor <= 0) {
            event.setCancelled(true);
            return;
        }

        event.setCooldown((int) Math.round(event.getCooldown() * factor));
    }

}
