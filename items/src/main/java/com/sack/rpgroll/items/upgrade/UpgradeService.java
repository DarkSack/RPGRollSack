package com.sack.rpgroll.items.upgrade;

import com.sack.rpgroll.api.RPGRollAPI;
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
        MISSING_MATERIAL
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

        UpgradeLevel upgrade = nextUpgrade.get();

        if (upgrade.cost() > 0 && !chargeMoney(player, upgrade.cost())) {
            return Result.CANT_AFFORD_MONEY;
        }

        if (upgrade.costMaterial() != null && !chargeMaterial(player, upgrade.costMaterial(), upgrade.costAmount())) {
            return Result.MISSING_MATERIAL;
        }

        ItemMeta meta = item.getItemMeta();
        instanceService.setUpgradeLevel(meta, nextLevel);
        item.setItemMeta(meta);

        itemFactory.rebuildDisplay(item, definition);

        return Result.OK;
    }

    private boolean chargeMoney(Player player, double amount) {

        if (!RPGRollAPI.isReady()) {
            return true;
        }

        var economyProvider = RPGRollAPI.get().getEconomyProvider();
        if (!economyProvider.isAvailable()) {
            return true;
        }

        var economy = economyProvider.getEconomy().orElseThrow();

        if (economy.getBalance(player) < amount) {
            return false;
        }

        economy.withdrawPlayer(player, amount);
        return true;
    }

    private boolean chargeMaterial(Player player, String materialName, int amount) {

        Material material;
        try {
            material = Material.valueOf(materialName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return true;
        }

        int available = player.getInventory().all(material).values().stream()
                .mapToInt(ItemStack::getAmount).sum();

        if (available < amount) {
            return false;
        }

        int remaining = amount;
        for (ItemStack stack : player.getInventory().all(material).values()) {

            if (remaining <= 0) {
                break;
            }

            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
        }

        return true;
    }

}
