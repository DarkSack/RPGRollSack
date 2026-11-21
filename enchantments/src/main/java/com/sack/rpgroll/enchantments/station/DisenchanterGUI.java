package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Desencantador: arriba el ítem de la mano; debajo, un libro por encantamiento. Clic en uno para quitarlo. */
final class DisenchanterGUI extends InventoryGUI {

    private static final int ITEM = 4;
    private static final int FIRST = 9;
    private static final int LAST = 35;
    private static final int CLOSE = 40;

    private final Disenchanter disenchanter;
    private final LangManager lang;
    private final Map<Integer, Disenchanter.Option> optionSlots = new HashMap<>();
    private ItemStack item;

    DisenchanterGUI(Player player, Disenchanter disenchanter, LangManager lang) {
        super(player, lang.component("station.disenchanter.title"), 45);
        this.disenchanter = disenchanter;
        this.lang = lang;
    }

    private static Component plain(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public void build() {
        clear();
        optionSlots.clear();
        item = player.getInventory().getItemInMainHand().clone();
        setItem(ITEM, item.clone());

        int slot = FIRST;
        int shards = disenchanter.countShards(player);
        for (Disenchanter.Option option : disenchanter.options(item)) {
            if (slot > LAST) {
                break;
            }
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("station.disenchanter.pay_money", "price",
                    disenchanter.formatMoney(disenchanter.moneyCost(option))));
            lore.add(lang.component("station.disenchanter.pay_shards", "amount", disenchanter.shardCost(option),
                    "have", shards));
            if (option.premium()) {
                lore.add(lang.component("station.disenchanter.premium"));
            }
            ItemStack icon = ItemStack.of(Material.ENCHANTED_BOOK);
            var meta = icon.getItemMeta();
            meta.displayName(plain(option.name()));
            meta.lore(lore.stream().map(DisenchanterGUI::plain).toList());
            icon.setItemMeta(meta);
            setItem(slot, icon);
            optionSlots.put(slot, option);
            slot++;
        }

        ItemStack close = ItemStack.of(Material.BARRIER);
        var meta = close.getItemMeta();
        meta.displayName(plain(lang.component("station.close")));
        close.setItemMeta(meta);
        setItem(CLOSE, close);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == CLOSE) {
            close();
            return;
        }
        Disenchanter.Option option = optionSlots.get(slot);
        if (option == null || !(event.isLeftClick() || event.isRightClick())) {
            return;
        }
        String result = disenchanter.disenchant(player, item, option, event.isRightClick());
        player.sendActionBar(lang.component(result));
        if (!result.endsWith("done")) {
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
        }
        if (disenchanter.options(player.getInventory().getItemInMainHand()).isEmpty()) {
            close();
        } else {
            build();
        }
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        // Nada entra ni sale: trabaja sobre lo que llevas en la mano.
        event.setCancelled(true);
    }
}
