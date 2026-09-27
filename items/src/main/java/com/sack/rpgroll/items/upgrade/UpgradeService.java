package com.sack.rpgroll.items.upgrade;

import com.sack.rpgroll.common.integration.VaultEconomy;
import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.UpgradeLevel;
import com.sack.rpgroll.items.instance.ItemInstanceService;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.Optional;

/**
 * Sube de nivel un ítem ("Espada +1" -&gt; "+2" -&gt; ...), cobrando el
 * costo definido para ese nivel (dinero vía Vault y/o un material) y
 * reaplicando stats/apariencia/rareza según {@link UpgradeLevel}.
 */
public class UpgradeService {

    public enum Result {
        OK,
        MAX_LEVEL,
        NO_UPGRADE_DEFINED,
        CANT_AFFORD_MONEY,
        MISSING_MATERIAL,
        ONE_AT_A_TIME
    }

    private final ItemInstanceService instanceService;
    private final ItemFactory itemFactory;

    public UpgradeService(ItemInstanceService instanceService, ItemFactory itemFactory) {
        this.instanceService = instanceService;
        this.itemFactory = itemFactory;
    }

    public Result upgrade(Player player, ItemStack item, ItemDefinition definition) {

        int currentLevel = instanceService.getUpgradeLevel(item);
        int nextLevel = currentLevel + 1;

        Optional<UpgradeLevel> nextUpgrade = definition.upgrades().stream()
                .filter(u -> u.level() == nextLevel)
                .findFirst();

        if (nextUpgrade.isEmpty()) {
            boolean anyHigherDefined = definition.upgrades().stream().anyMatch(u -> u.level() > currentLevel);
            return anyHigherDefined ? Result.NO_UPGRADE_DEFINED : Result.MAX_LEVEL;
        }

        // Un stack entero subía de nivel pagando una sola mejora.
        if (item.getAmount() > 1) {
            return Result.ONE_AT_A_TIME;
        }

        UpgradeLevel upgrade = nextUpgrade.get();
        Material costMaterial = parseMaterial(upgrade.costMaterial());

        // Todo se comprueba antes de cobrar nada: antes el dinero se descontaba
        // primero y, si faltaba el material, se perdía sin mejorar el ítem.
        if (upgrade.cost() > 0 && !canAfford(player, upgrade.cost())) {
            return Result.CANT_AFFORD_MONEY;
        }

        if (costMaterial != null && countMaterial(player, costMaterial, item) < upgrade.costAmount()) {
            return Result.MISSING_MATERIAL;
        }

        if (upgrade.cost() > 0 && !chargeMoney(player, upgrade.cost())) {
            return Result.CANT_AFFORD_MONEY;
        }

        if (costMaterial != null) {
            takeMaterial(player, costMaterial, upgrade.costAmount(), item);
        }

        ItemMeta meta = item.getItemMeta();
        instanceService.setUpgradeLevel(meta, nextLevel);
        item.setItemMeta(meta);

        itemFactory.rebuildDisplay(item, definition);

        return Result.OK;
    }

    private boolean canAfford(Player player, double amount) {
        return VaultEconomy.get().map(economy -> economy.has(player, amount)).orElse(true);
    }

    private boolean chargeMoney(Player player, double amount) {

        var found = VaultEconomy.get();
        if (found.isEmpty()) {
            return true;
        }

        var economy = found.get();

        if (!economy.has(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    private Material parseMaterial(String materialName) {

        if (materialName == null) {
            return null;
        }

        try {
            return Material.valueOf(materialName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Solo cuenta el material normal de la mochila y la barra: ni los ítems
     * custom hechos sobre ese material (una gema sobre DIAMOND no es un
     * diamante), ni la armadura puesta, ni el propio ítem que se mejora.
     */
    private boolean isPayment(ItemStack stack, Material material, ItemStack upgrading) {
        return stack != null && stack != upgrading && stack.getType() == material
                && instanceService.getId(stack).isEmpty();
    }

    private int countMaterial(Player player, Material material, ItemStack upgrading) {

        int available = 0;

        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isPayment(stack, material, upgrading)) {
                available += stack.getAmount();
            }
        }

        return available;
    }

    private void takeMaterial(Player player, Material material, int amount, ItemStack upgrading) {

        int remaining = amount;

        for (ItemStack stack : player.getInventory().getStorageContents()) {

            if (remaining <= 0) {
                break;
            }

            if (!isPayment(stack, material, upgrading)) {
                continue;
            }

            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
        }
    }

}
