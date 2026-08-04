package com.sack.rpgroll.enchantments.listener;

import com.sack.rpgroll.enchantments.condition.ConditionContext;
import com.sack.rpgroll.enchantments.condition.ConditionEvaluator;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.core.Trigger;
import com.sack.rpgroll.enchantments.effect.EffectContext;
import com.sack.rpgroll.enchantments.effect.EnchantEffectExecutor;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Traduce eventos de Bukkit/Paper a {@link Trigger}s del sistema de
 * encantamientos: por cada trigger, revisa todos los ítems relevantes que
 * el jugador tiene puestos (mano principal, secundaria y armadura) y, por
 * cada encantamiento que declare ese trigger, evalúa probabilidad,
 * condiciones y finalmente ejecuta sus efectos.
 */
public class EnchantmentTriggerListener implements Listener {

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
        handleTrigger(Trigger.PLAYER_ATTACK, attacker, target);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        LivingEntity source = event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof LivingEntity living ? living : null;

        handleTrigger(Trigger.ENTITY_DAMAGE, victim, source);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleTrigger(Trigger.BLOCK_BREAK, event.getPlayer(), null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        handleTrigger(Trigger.PLAYER_JUMP, event.getPlayer(), null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        handleTrigger(Trigger.PLAYER_MOVE, event.getPlayer(), null);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        handleTrigger(Trigger.PLAYER_DEATH, event.getEntity(), event.getEntity().getKiller());
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {

        if (!(event.getEntity().getKiller() instanceof Player killer)) {
            return;
        }

        handleTrigger(Trigger.ENTITY_KILL, killer, event.getEntity());
    }

    private void handleTrigger(Trigger trigger, Player player, LivingEntity target) {

        for (ItemStack item : relevantItems(player)) {

            if (item == null || item.getType().isAir()) {
                continue;
            }

            Map<String, Integer> enchantments = enchantmentItem.getAll(item);

            for (var entry : enchantments.entrySet()) {
                applyIfMatches(trigger, player, target, entry.getKey(), entry.getValue());
            }
        }
    }

    private void applyIfMatches(Trigger trigger, Player player, LivingEntity target, String enchantId, int level) {

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

            EffectContext effectContext = new EffectContext(player, target, level, enchantment.levelData(level));
            effectExecutor.execute(enchantment.effects(), effectContext);
        });
    }

    private List<ItemStack> relevantItems(Player player) {

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

}
