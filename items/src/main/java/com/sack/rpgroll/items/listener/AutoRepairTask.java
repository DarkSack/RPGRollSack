package com.sack.rpgroll.items.listener;

import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.durability.DurabilityService;
import com.sack.rpgroll.items.instance.ItemInstanceService;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Corre una vez por minuto: auto-repara los ítems equipados que lo tengan configurado. */
public class AutoRepairTask implements Runnable {

    private final ItemManager itemManager;
    private final ItemInstanceService instanceService;
    private final DurabilityService durabilityService;

    public AutoRepairTask(ItemManager itemManager, ItemInstanceService instanceService,
            DurabilityService durabilityService) {
        this.itemManager = itemManager;
        this.instanceService = instanceService;
        this.durabilityService = durabilityService;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player);
        }
    }

    private void checkPlayer(Player player) {

        PlayerInventory inventory = player.getInventory();

        ItemStack[] items = {
                inventory.getItemInMainHand(), inventory.getItemInOffHand(),
                inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()
        };

        for (ItemStack item : items) {

            if (item == null || item.getType().isAir()) {
                continue;
            }

            instanceService.getId(item).flatMap(itemManager::get)
                    .ifPresent(definition -> durabilityService.autoRepairTick(item, definition));
        }
    }

}
