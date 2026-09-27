package com.sack.rpgroll.furniture.gui;

import com.sack.rpgroll.common.integration.VaultEconomy;
import com.sack.rpgroll.furniture.core.CarpenterRecipe;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.item.FurnitureItems;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Cobrar una receta del carpintero: materiales del inventario y dinero por Vault.
 * <p>
 * Solo cuentan los materiales "limpios" (sin nombre, lore ni datos de otro plugin): una tabla de
 * roble renombrada o un ítem de RPGRoll-Items con material base de tabla no se gastan como
 * tablas normales.
 */
public final class Carpentry {

    public enum Result {
        OK,
        MISSING_MATERIALS,
        NO_MONEY,
        NO_ECONOMY
    }

    private Carpentry() {
    }

    public static boolean isPlain(ItemStack item, Material material) {
        return item != null && item.getType() == material && !item.hasItemMeta();
    }

    public static int count(PlayerInventory inventory, Material material) {

        int total = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (isPlain(item, material)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static Result check(Player player, CarpenterRecipe recipe) {

        for (Map.Entry<Material, Integer> entry : recipe.materials().entrySet()) {
            if (count(player.getInventory(), entry.getKey()) < entry.getValue()) {
                return Result.MISSING_MATERIALS;
            }
        }

        if (recipe.money() > 0) {
            var economy = VaultEconomy.get();
            if (economy.isEmpty()) {
                return Result.NO_ECONOMY;
            }
            if (!economy.get().has(player, recipe.money())) {
                return Result.NO_MONEY;
            }
        }
        return Result.OK;
    }

    /** Cobra y entrega. Comprueba otra vez antes de tocar nada. */
    public static Result craft(Player player, FurnitureDefinition def, String variant, CarpenterRecipe recipe,
            FurnitureItems items) {

        Result check = check(player, recipe);
        if (check != Result.OK) {
            return check;
        }

        if (recipe.money() > 0) {
            var response = VaultEconomy.get().orElseThrow().withdrawPlayer(player, recipe.money());
            if (!response.transactionSuccess()) {
                return Result.NO_MONEY;
            }
        }

        PlayerInventory inventory = player.getInventory();
        for (Map.Entry<Material, Integer> entry : recipe.materials().entrySet()) {
            int remaining = entry.getValue();
            ItemStack[] contents = inventory.getStorageContents();
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                ItemStack item = contents[slot];
                if (!isPlain(item, entry.getKey())) {
                    continue;
                }
                int take = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - take);
                inventory.setItem(slot, item.getAmount() <= 0 ? null : item);
                remaining -= take;
            }
        }

        ItemStack result = items.create(def, variant, recipe.amount());
        inventory.addItem(result).values().forEach(left ->
                player.getWorld().dropItemNaturally(player.getLocation(), left));
        return Result.OK;
    }
}
