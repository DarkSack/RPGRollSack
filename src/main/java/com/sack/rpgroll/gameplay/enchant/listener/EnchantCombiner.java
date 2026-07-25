package com.sack.rpgroll.gameplay.enchant.listener;

import com.sack.rpgroll.gameplay.enchant.CustomEnchantment;
import com.sack.rpgroll.gameplay.enchant.EnchantManager;
import com.sack.rpgroll.gameplay.enchant.ItemCategory;
import com.sack.rpgroll.gameplay.enchant.ItemEnchantmentStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * Permite combinar encantamientos custom en el yunque vanilla.
 * <p>
 * Regla: mismo encantamiento + mismo nivel en ambos items → sube 1 nivel
 * (tope maxLevel). Si el segundo item tiene nivel mayor, se toma ese nivel.
 * Si tiene nivel menor, no hace nada con ese encantamiento (no downgradea).
 * Un encantamiento nuevo en el segundo item (que el primero no tiene) se
 * agrega directo, si aplica a la categoría del item base.
 */
public class EnchantCombiner implements Listener {

    private static final int MAX_ANVIL_COST = 39;
    private static final int COST_PER_LEVEL = 2;

    private final ItemEnchantmentStorage storage;
    private final EnchantManager enchantManager;

    public EnchantCombiner(ItemEnchantmentStorage storage, EnchantManager enchantManager) {
        this.storage = storage;
        this.enchantManager = enchantManager;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {

        AnvilInventory inventory = event.getInventory();
        ItemStack base = inventory.getItem(0);
        ItemStack addition = inventory.getItem(1);

        if (base == null || addition == null) {
            return;
        }

        Map<String, Integer> additionEnchants = storage.getEnchantments(addition);

        if (additionEnchants.isEmpty()) {
            return; // No hay nada custom que combinar, dejar comportamiento vanilla intacto
        }

        Optional<ItemCategory> category = ItemCategory.fromMaterial(base.getType());
        Map<String, Integer> baseEnchants = storage.getEnchantments(base);

        ItemStack result = event.getResult() != null ? event.getResult().clone() : base.clone();

        boolean changed = false;
        int extraCost = 0;

        for (Map.Entry<String, Integer> entry : additionEnchants.entrySet()) {

            String enchantId = entry.getKey();
            int additionLevel = entry.getValue();

            Optional<CustomEnchantment> enchantOpt = enchantManager.get(enchantId);
            if (enchantOpt.isEmpty()) {
                continue;
            }

            CustomEnchantment enchant = enchantOpt.get();

            if (category.isEmpty() || !enchant.applicableTo().contains(category.get())) {
                continue;
            }

            int baseLevel = baseEnchants.getOrDefault(enchantId, 0);
            int newLevel;

            if (baseLevel == 0) {
                newLevel = additionLevel;
            } else if (baseLevel == additionLevel) {
                newLevel = Math.min(baseLevel + 1, enchant.maxLevel());
            } else if (additionLevel > baseLevel) {
                newLevel = Math.min(additionLevel, enchant.maxLevel());
            } else {
                continue; // El sacrificio tiene nivel menor, no aporta nada
            }

            if (newLevel == baseLevel) {
                continue; // Ya está al tope, o sin cambio real
            }

            storage.addEnchantment(result, enchantId, newLevel);
            changed = true;
            extraCost += newLevel * COST_PER_LEVEL;
        }

        if (!changed) {
            return;
        }

        int finalCost = Math.min(MAX_ANVIL_COST, Math.max(1, inventory.getRepairCost() + extraCost));
        inventory.setRepairCost(finalCost);
        event.setResult(result);
    }

}