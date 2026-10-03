package com.sack.rpgroll.enchantments.listener;

import com.sack.rpgroll.enchantments.condition.ConditionContext;
import com.sack.rpgroll.enchantments.condition.ConditionEvaluator;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.core.Trigger;
import com.sack.rpgroll.enchantments.effect.EffectContext;
import com.sack.rpgroll.enchantments.effect.EnchantEffectExecutor;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import io.papermc.paper.event.player.PlayerShieldDisableEvent;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Traduce eventos de Bukkit/Paper a {@link Trigger}s del sistema de
 * encantamientos: por cada trigger, revisa todos los ítems relevantes que
 * el jugador tiene puestos (mano principal, secundaria y armadura) y, por
 * cada encantamiento que declare ese trigger, evalúa probabilidad,
 * condiciones y finalmente ejecuta sus efectos.
 * <p>
 * Cada efecto recibe además el evento, el ítem encantado y su ranura: los de
 * herramienta solo actúan si el ítem está en la mano principal.
 */
public class EnchantmentTriggerListener implements Listener {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final EnchantmentManager manager;
    private final EnchantmentItem enchantmentItem;
    private final ConditionEvaluator conditionEvaluator;
    private final EnchantEffectExecutor effectExecutor;

    public EnchantmentTriggerListener(
            EnchantmentManager manager,
            EnchantmentItem enchantmentItem,
            ConditionEvaluator conditionEvaluator,
            EnchantEffectExecutor effectExecutor) {

        this.manager = manager;
        this.enchantmentItem = enchantmentItem;
        this.conditionEvaluator = conditionEvaluator;
        this.effectExecutor = effectExecutor;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        LivingEntity target = event.getEntity() instanceof LivingEntity living ? living : null;
        handleTrigger(Trigger.PLAYER_ATTACK, attacker, target, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        LivingEntity source = null;

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof LivingEntity living) {
                source = living;
            } else if (byEntity.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof LivingEntity shooter) {
                source = shooter;
            }
        }

        if (blockedByShield(victim, event)) {
            handleTrigger(Trigger.SHIELD_BLOCK, victim, source, event);
        }

        handleTrigger(Trigger.ENTITY_DAMAGE, victim, source, event);
    }

    /**
     * El escudo paró el golpe. BLOCKING sigue siendo la forma en que Paper
     * cuenta lo que quita el escudo, aunque esté marcado como obsoleto.
     */
    @SuppressWarnings("deprecation")
    private static boolean blockedByShield(Player victim, EntityDamageEvent event) {
        return victim.isBlocking()
                && event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)
                && event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
    }

    @EventHandler(ignoreCancelled = true)
    public void onShieldDisable(PlayerShieldDisableEvent event) {
        LivingEntity source = event.getDamager() instanceof LivingEntity living ? living : null;
        handleTrigger(Trigger.SHIELD_DISABLE, event.getPlayer(), source, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleTrigger(Trigger.BLOCK_BREAK, event.getPlayer(), null, event);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.useInteractedBlock() == Event.Result.DENY || event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        handleTrigger(Trigger.BLOCK_INTERACT, event.getPlayer(), null, event);
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {

        if (event.getAmount() > 0) {
            handleTrigger(Trigger.EXP_PICKUP, event.getPlayer(), null, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        handleTrigger(Trigger.PLAYER_JUMP, event.getPlayer(), null, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        handleTrigger(Trigger.PLAYER_MOVE, event.getPlayer(), null, event);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        handleTrigger(Trigger.PLAYER_DEATH, event.getEntity(), event.getEntity().getKiller(), event);
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {

        if (!(event.getEntity().getKiller() instanceof Player killer)) {
            return;
        }

        handleTrigger(Trigger.ENTITY_KILL, killer, event.getEntity(), event);
    }

    private void handleTrigger(Trigger trigger, Player player, LivingEntity target, Event event) {

        PlayerInventory inventory = player.getInventory();

        for (EquipmentSlot slot : SLOTS) {

            ItemStack item = inventory.getItem(slot);

            if (item == null || item.getType().isAir()) {
                continue;
            }

            Map<String, Integer> enchantments = enchantmentItem.getAll(item);

            for (var entry : enchantments.entrySet()) {
                applyIfMatches(trigger, player, target, entry.getKey(), entry.getValue(), item, slot, event);
            }
        }
    }

    private void applyIfMatches(Trigger trigger, Player player, LivingEntity target, String enchantId, int level,
                                ItemStack item, EquipmentSlot slot, Event event) {

        manager.get(enchantId).ifPresent(enchantment -> {

            if (!enchantment.triggers().contains(trigger)) {
                return;
            }

            if (Math.random() * 100 >= enchantment.chance()) {
                return;
            }

            ConditionContext conditionContext = new ConditionContext(player, target);

            if (!conditionEvaluator.evaluateAll(enchantment.conditions(), conditionContext)) {
                return;
            }

            EffectContext effectContext = new EffectContext(player, target, level, enchantment.levelData(level),
                    item, slot, event);
            effectExecutor.execute(enchantment.effects(), effectContext);
        });
    }

}
