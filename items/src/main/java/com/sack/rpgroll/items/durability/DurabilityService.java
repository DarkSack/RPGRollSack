package com.sack.rpgroll.items.durability;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.instance.ItemInstanceService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Durabilidad propia de RPGRoll-Items — no toca la durabilidad vanilla.
 * Degrada por uso (ENTITY_HIT, BLOCK_BREAK, según config del ítem),
 * permite reparar, y rompe (consume) el ítem al llegar a 0.
 */
public class DurabilityService {

    private final ItemInstanceService instanceService;
    private final ItemFactory itemFactory;

    public DurabilityService(ItemInstanceService instanceService, ItemFactory itemFactory) {
        this.instanceService = instanceService;
        this.itemFactory = itemFactory;
    }

    /** @return true si el ítem se rompió (el caller debe quitarlo del inventario). */
    public boolean degrade(Player player, ItemStack item, ItemDefinition definition, int amount) {

        if (!definition.durability().enabled()) {
            return false;
        }

        int current = instanceService.getDurability(item, definition.durability().maxDurability());
        int updated = Math.max(0, current - amount);

        writeDurability(item, definition, updated);

        if (updated <= 0) {
            item.setAmount(0);
            player.sendMessage(Component.text(
                    "Tu " + definition.displayName() + " se rompió.", NamedTextColor.RED));
            return true;
        }

        return false;
    }

    public void repair(ItemStack item, ItemDefinition definition, int amount) {

        if (!definition.durability().enabled() || !definition.durability().repairable()) {
            return;
        }

        int current = instanceService.getDurability(item, definition.durability().maxDurability());
        int updated = Math.min(definition.durability().maxDurability(), current + amount);

        writeDurability(item, definition, updated);
    }

    public void autoRepairTick(ItemStack item, ItemDefinition definition) {

        if (!definition.durability().enabled() || definition.durability().autoRepairPerMinute() <= 0) {
            return;
        }

        int current = instanceService.getDurability(item, definition.durability().maxDurability());

        if (current >= definition.durability().maxDurability()) {
            return;
        }

        // Se llama una vez por minuto (ver ItemPollingTask) — sumar directo la tasa configurada.
        repair(item, definition, definition.durability().autoRepairPerMinute());
    }

    private void writeDurability(ItemStack item, ItemDefinition definition, int value) {

        ItemMeta meta = item.getItemMeta();
        instanceService.setDurability(meta, value);
        item.setItemMeta(meta);

        itemFactory.rebuildDisplay(item, definition);
    }

}
