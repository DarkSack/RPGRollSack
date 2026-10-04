package com.sack.rpgroll.machines.core;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.menu.PaymentItems;

import net.kyori.adventure.text.Component;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Comprobar y cobrar un {@link Cost}. */
public final class Payment {

    /** La clave con la que RPGRoll-Items marca sus ítems (namespace = nombre del plugin). */
    private static final NamespacedKey ITEM_ID = new NamespacedKey("rpgroll-items", "item-id");

    private Payment() {
    }

    public static boolean matches(ItemStack stack, Cost.Item item) {

        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        if (item.custom()) {
            if (!stack.hasItemMeta()) {
                return false;
            }
            String id = stack.getItemMeta().getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING);
            return item.itemId().equalsIgnoreCase(id);
        }
        return stack.getType() == item.material() && PaymentItems.isPlain(stack);
    }

    public static int count(Player player, Cost.Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (matches(stack, item)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public static boolean has(Player player, Cost cost) {

        if (cost.money() > 0 && !Money.has(player, cost.money())) {
            return false;
        }
        return cost.items().stream().allMatch(item -> count(player, item) >= item.amount());
    }

    /** Cobra si puede; si no, no toca nada. */
    public static boolean take(Player player, Cost cost) {

        if (!has(player, cost)) {
            return false;
        }
        if (cost.money() > 0 && !Money.take(player, cost.money())) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        for (Cost.Item item : cost.items()) {
            int left = item.amount();
            ItemStack[] contents = inventory.getStorageContents();
            for (int slot = 0; slot < contents.length && left > 0; slot++) {
                ItemStack stack = contents[slot];
                if (!matches(stack, item)) {
                    continue;
                }
                int taken = Math.min(left, stack.getAmount());
                stack.setAmount(stack.getAmount() - taken);
                contents[slot] = stack.getAmount() <= 0 ? null : stack;
                left -= taken;
            }
            inventory.setStorageContents(contents);
        }
        return true;
    }

    /** Las líneas de lore del coste, cada una en verde o en rojo según si el jugador lo tiene. */
    public static List<Component> lore(Player player, Cost cost, LangManager lang) {

        List<Component> lines = new ArrayList<>();
        if (cost.free()) {
            lines.add(lang.component("cost.free"));
            return lines;
        }
        lines.add(lang.component("cost.header"));
        if (cost.money() > 0) {
            boolean ok = Money.available() && Money.has(player, cost.money());
            lines.add(lang.component(ok ? "cost.money_ok" : "cost.money_missing", "amount", Money.format(cost.money())));
        }
        for (Cost.Item item : cost.items()) {
            int have = count(player, item);
            lines.add(lang.component(have >= item.amount() ? "cost.item_ok" : "cost.item_missing",
                    "amount", item.amount(), "item", item.label(), "have", have));
        }
        return lines;
    }
}
