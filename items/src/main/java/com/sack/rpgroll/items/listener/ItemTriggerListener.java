package com.sack.rpgroll.items.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import com.sack.rpgroll.items.condition.ItemConditionContext;
import com.sack.rpgroll.items.condition.ItemConditionEvaluator;
import com.sack.rpgroll.items.core.ItemAbility;
import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemTrigger;
import com.sack.rpgroll.items.durability.DurabilityService;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.registry.ActionRegistry;
import com.sack.rpgroll.items.registry.ItemActionContext;
import com.sack.rpgroll.items.stat.ItemStatEngine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Traduce eventos de Bukkit/Paper a triggers de ítem: ejecuta el
 * comportamiento fijo del ítem ({@code definition.actionsFor(trigger)}) y,
 * para RIGHT_CLICK/LEFT_CLICK, además evalúa sus habilidades activas
 * (cooldown propio + condiciones antes de ejecutar). También degrada
 * durabilidad en ENTITY_HIT/BLOCK_BREAK.
 */
public class ItemTriggerListener implements Listener {

    private final ItemManager itemManager;
    private final ItemInstanceService instanceService;
    private final ActionRegistry actionRegistry;
    private final ItemConditionEvaluator conditionEvaluator;
    private final DurabilityService durabilityService;
    private final ItemStatEngine statEngine;

    private final Map<String, Long> abilityCooldowns = new HashMap<>();

    public ItemTriggerListener(
            ItemManager itemManager,
            ItemInstanceService instanceService,
            ActionRegistry actionRegistry,
            ItemConditionEvaluator conditionEvaluator,
            DurabilityService durabilityService,
            ItemStatEngine statEngine) {

        this.itemManager = itemManager;
        this.instanceService = instanceService;
        this.actionRegistry = actionRegistry;
        this.conditionEvaluator = conditionEvaluator;
        this.durabilityService = durabilityService;
        this.statEngine = statEngine;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        ItemStack heldItem = event.getItem();

        if (event.getAction() == Action.PHYSICAL) {
            fireTrigger(ItemTrigger.PLAYER_INTERACT, event.getPlayer(), heldItem, null);
            return;
        }

        ItemTrigger trigger = switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> ItemTrigger.RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> ItemTrigger.LEFT_CLICK;
            default -> null;
        };

        if (trigger != null) {
            fireTrigger(trigger, event.getPlayer(), heldItem, null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        LivingEntity target = event.getEntity() instanceof LivingEntity living ? living : null;
        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        fireTrigger(ItemTrigger.ENTITY_HIT, attacker, weapon, target);

        applyWeaponDamageBonus(event, attacker);
        degradeIfApplicable(attacker, weapon);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        applyArmorDefenseReduction(event, victim);

        for (ItemStack armorPiece : victim.getInventory().getArmorContents()) {
            fireTrigger(ItemTrigger.PLAYER_DAMAGE, victim, armorPiece, null);
        }
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {

        if (!(event.getEntity().getKiller() instanceof Player killer)) {
            return;
        }

        fireTrigger(ItemTrigger.ENTITY_KILL, killer, killer.getInventory().getItemInMainHand(), event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        fireTrigger(ItemTrigger.BLOCK_BREAK, event.getPlayer(), tool, null);
        degradeIfApplicable(event.getPlayer(), tool);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        fireTrigger(ItemTrigger.BLOCK_PLACE, event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand(),
                null);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        fireTrigger(ItemTrigger.PLAYER_DEATH, event.getEntity(), null, null);
        statEngine.clear(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        fireTrigger(ItemTrigger.PLAYER_RESPAWN, event.getPlayer(), null, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        fireTrigger(ItemTrigger.PLAYER_MOVE, event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand(),
                null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        fireTrigger(ItemTrigger.PLAYER_JUMP, event.getPlayer(), event.getPlayer().getInventory().getBoots(), null);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            fireTrigger(ItemTrigger.PLAYER_SNEAK, event.getPlayer(),
                    event.getPlayer().getInventory().getItemInMainHand(), null);
        }
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        if (event.isSprinting()) {
            fireTrigger(ItemTrigger.PLAYER_SPRINT, event.getPlayer(),
                    event.getPlayer().getInventory().getBoots(), null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        fireTrigger(ItemTrigger.CONSUME, event.getPlayer(), event.getItem(), null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onThrow(ProjectileLaunchEvent event) {

        if (!(event.getEntity().getShooter() instanceof Player thrower)) {
            return;
        }

        fireTrigger(ItemTrigger.THROW, thrower, thrower.getInventory().getItemInMainHand(), null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        fireTrigger(ItemTrigger.PICKUP, player, event.getItem().getItemStack(), null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        fireTrigger(ItemTrigger.DROP, event.getPlayer(), event.getItemDrop().getItemStack(), null);
    }

    // ============ Núcleo ============

    private void fireTrigger(ItemTrigger trigger, Player player, ItemStack item, LivingEntity target) {

        if (item == null || item.getType().isAir()) {
            return;
        }

        Optional<String> idOpt = instanceService.getId(item);
        if (idOpt.isEmpty()) {
            return;
        }

        Optional<ItemDefinition> definitionOpt = itemManager.get(idOpt.get());
        if (definitionOpt.isEmpty()) {
            return;
        }

        ItemDefinition definition = definitionOpt.get();
        ItemActionContext context = new ItemActionContext(player, item, definition, target);

        actionRegistry.executeAll(definition.actionsFor(trigger), context);

        for (ItemAbility ability : definition.abilities()) {
            if (!ability.passive() && ability.trigger() == trigger) {
                tryActivateAbility(ability, player, context);
            }
        }
    }

    private void tryActivateAbility(ItemAbility ability, Player player, ItemActionContext context) {

        String cooldownKey = player.getUniqueId() + ":" + ability.id();
        long now = System.currentTimeMillis();
        Long expiry = abilityCooldowns.get(cooldownKey);

        if (expiry != null && expiry > now) {
            long remainingSeconds = (expiry - now) / 1000 + 1;
            player.sendMessage(Component.text(
                    ability.displayName() + " en cooldown (" + remainingSeconds + "s).", NamedTextColor.RED));
            return;
        }

        ItemConditionContext conditionContext = new ItemConditionContext(player, context.target());

        if (!conditionEvaluator.evaluateAll(ability.conditions(), conditionContext)) {
            return;
        }

        actionRegistry.executeAll(ability.actions(), context);
        abilityCooldowns.put(cooldownKey, now + ability.cooldownMillis());
    }

    private void degradeIfApplicable(Player player, ItemStack item) {

        if (item == null || item.getType().isAir()) {
            return;
        }

        instanceService.getId(item).flatMap(itemManager::get).ifPresent(definition -> {
            if (definition.durability().enabled()) {
                durabilityService.degrade(player, item, definition, definition.durability().degradePerUse());
            }
        });
    }

    private void applyWeaponDamageBonus(EntityDamageByEntityEvent event, Player attacker) {

        double damage = statEngine.getStat(attacker, "damage");
        double critChance = statEngine.getStat(attacker, "critical_chance");
        double critDamage = statEngine.getStat(attacker, "critical_damage");

        double bonus = damage;

        if (critChance > 0 && Math.random() * 100 < critChance) {
            bonus += event.getDamage() * (critDamage / 100.0);
        }

        if (bonus > 0) {
            event.setDamage(event.getDamage() + bonus);
        }
    }

    private void applyArmorDefenseReduction(EntityDamageEvent event, Player victim) {

        double defense = statEngine.getStat(victim, "defense") + statEngine.getStat(victim, "resistance");

        if (defense <= 0) {
            return;
        }

        double reduced = Math.max(0, event.getDamage() - defense);
        event.setDamage(reduced);
    }

}
