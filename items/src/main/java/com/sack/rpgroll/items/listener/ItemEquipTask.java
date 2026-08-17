package com.sack.rpgroll.items.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.items.core.ItemAbility;
import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemEffectDef;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.core.ItemTrigger;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.registry.ActionRegistry;
import com.sack.rpgroll.items.registry.ItemActionContext;
import com.sack.rpgroll.items.requirement.ItemRequirementChecker;
import com.sack.rpgroll.items.stat.ItemStatEngine;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bukkit no tiene un evento único que cubra equipar/desequipar en las 4
 * ranuras de armadura + mano principal/secundaria (comandos, dispensers,
 * clicks de inventario, etc. lo cambian todo de formas distintas) — así
 * que se detecta por polling: cada 10 ticks se compara el ítem de cada
 * ranura contra el último conocido y se disparan EQUIP/UNEQUIP en los
 * cambios. En la misma pasada se recalculan stats, se refrescan los
 * efectos "mientras está equipado" y se ejecutan las habilidades pasivas
 * (una vez, al equiparse).
 */
public class ItemEquipTask implements Runnable {

    private static final int EFFECT_REFRESH_TICKS = 30;
    private static final String[] SLOTS = {"mainhand", "offhand", "helmet", "chestplate", "leggings", "boots"};

    private final ItemManager itemManager;
    private final ItemInstanceService instanceService;
    private final ActionRegistry actionRegistry;
    private final ItemStatEngine statEngine;
    private final ItemRequirementChecker requirementChecker;
    private final LangManager langManager;

    private final Map<UUID, Map<String, String>> lastKnown = new HashMap<>();

    public ItemEquipTask(ItemManager itemManager, ItemInstanceService instanceService, ActionRegistry actionRegistry,
            ItemStatEngine statEngine, ItemRequirementChecker requirementChecker, LangManager langManager) {
        this.itemManager = itemManager;
        this.instanceService = instanceService;
        this.actionRegistry = actionRegistry;
        this.statEngine = statEngine;
        this.requirementChecker = requirementChecker;
        this.langManager = langManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player);
        }
    }

    private void checkPlayer(Player player) {

        PlayerInventory inventory = player.getInventory();

        ItemStack[] current = {
                inventory.getItemInMainHand(), inventory.getItemInOffHand(),
                inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()
        };

        Map<String, String> previous = lastKnown.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        boolean changed = false;

        for (int i = 0; i < SLOTS.length; i++) {

            String slot = SLOTS[i];
            ItemStack currentItem = current[i];
            String currentId = instanceService.getId(currentItem).orElse(null);
            String previousId = previous.get(slot);

            if (Objects.equals(currentId, previousId)) {
                continue;
            }

            changed = true;

            if (previousId != null) {
                itemManager.get(previousId).ifPresent(def -> fireEquipTrigger(ItemTrigger.UNEQUIP, player, null, def));
            }

            if (currentId != null) {
                itemManager.get(currentId).ifPresent(def -> onEquip(player, currentItem, def));
            }

            if (currentId != null) {
                previous.put(slot, currentId);
            } else {
                previous.remove(slot);
            }
        }

        if (changed) {
            statEngine.recompute(player);
        }

        refreshHeldEffects(player, current);
    }

    private void onEquip(Player player, ItemStack item, ItemDefinition definition) {

        fireEquipTrigger(ItemTrigger.EQUIP, player, item, definition);

        List<String> unmet = requirementChecker.check(player, definition.requirements());
        if (!unmet.isEmpty()) {
            langManager.send(player, "equip.requirements_header", "item", definition.displayName());
            unmet.forEach(reason -> langManager.send(player, "equip.requirement_line", "reason", reason));
        }

        for (ItemAbility ability : definition.abilities()) {
            if (ability.passive()) {
                actionRegistry.executeAll(ability.actions(), new ItemActionContext(player, item, definition, null));
            }
        }
    }

    private void fireEquipTrigger(ItemTrigger trigger, Player player, ItemStack item, ItemDefinition definition) {
        actionRegistry.executeAll(definition.actionsFor(trigger), new ItemActionContext(player, item, definition, null));
    }

    private void refreshHeldEffects(Player player, ItemStack[] currentItems) {

        for (ItemStack item : currentItems) {

            if (item == null || item.getType().isAir()) {
                continue;
            }

            instanceService.getId(item).flatMap(itemManager::get).ifPresent(definition -> {
                for (ItemEffectDef effect : definition.effects()) {
                    if (effect.requiresHeld()) {
                        applyEffect(player, effect);
                    }
                }
            });
        }
    }

    private void applyEffect(Player player, ItemEffectDef effect) {

        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effect.potion().toLowerCase()));

        if (type == null) {
            return;
        }

        player.addPotionEffect(new PotionEffect(type, EFFECT_REFRESH_TICKS + 5, effect.amplifier(), true, false));
    }

    public void clear(Player player) {
        lastKnown.remove(player.getUniqueId());
        statEngine.clear(player);
    }

}
