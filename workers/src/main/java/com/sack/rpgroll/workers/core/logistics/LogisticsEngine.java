package com.sack.rpgroll.workers.core.logistics;

import com.sack.rpgroll.workers.core.worker.Worker;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Vacía el inventario cargado de un worker en el {@code Container} vanilla real que hay debajo del almacén. */
public class LogisticsEngine {

    public void deposit(Worker worker, Warehouse warehouse) {

        Block block = warehouse.location().getBlock();

        if (!(block.getState() instanceof Container container)) {
            return;
        }

        for (String materialName : List.copyOf(worker.carriedItems().keySet())) {

            if (!warehouse.accepts(materialName)) {
                continue;
            }

            Material material = parseMaterial(materialName);

            if (material == null) {
                continue;
            }

            int amount = worker.carriedItems().getOrDefault(materialName, 0);

            if (amount <= 0) {
                continue;
            }

            Map<Integer, ItemStack> leftover = container.getInventory().addItem(new ItemStack(material, amount));
            int leftoverAmount = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            int stored = amount - leftoverAmount;

            if (stored >= amount) {
                worker.carriedItems().remove(materialName);
            } else {
                worker.carriedItems().put(materialName, leftoverAmount);
            }
        }
    }

    private Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
