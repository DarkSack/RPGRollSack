package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.servershop.ServerShopCategory;
import com.sack.rpgroll.economy.servershop.ServerShopManager;
import com.sack.rpgroll.economy.servershop.ServerShopService;
import com.sack.rpgroll.economy.wallet.WalletService;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Portada de la tienda del servidor: una casilla por sección, en el slot que diga su YAML. */
public class ServerShopGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int CONTENT = 45;
    private static final int BALANCE = 49;
    private static final int CLOSE = 53;

    private final ServerShopManager shops;
    private final ServerShopService service;
    private final WalletService wallet;
    private final LangManager lang;
    private final Map<Integer, ServerShopCategory> bySlot = new HashMap<>();

    public ServerShopGUI(Player player, ServerShopManager shops, ServerShopService service, WalletService wallet,
            LangManager lang) {
        super(player, lang.component("server_shop.title"), SIZE);
        this.shops = shops;
        this.service = service;
        this.wallet = wallet;
        this.lang = lang;
    }

    @Override
    public void build() {

        clear();
        bySlot.clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<ServerShopCategory> pending = new ArrayList<>();

        for (ServerShopCategory category : shops.ordered()) {
            if (category.slot() >= 0 && category.slot() < CONTENT && !bySlot.containsKey(category.slot())) {
                bySlot.put(category.slot(), category);
            } else {
                pending.add(category);
            }
        }

        // Las que no dicen slot (o repiten uno) van al primer hueco libre.
        int free = 0;
        for (ServerShopCategory category : pending) {
            while (free < CONTENT && bySlot.containsKey(free)) {
                free++;
            }
            if (free < CONTENT) {
                bySlot.put(free, category);
            }
        }

        bySlot.forEach((slot, category) -> setItem(slot, icon(category)));

        if (bySlot.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER).setName(lang.component("server_shop.no_categories"))
                    .build());
        }

        var currency = service.currency(null);
        setItem(BALANCE, new ItemBuilder(Material.SUNFLOWER)
                .setName(lang.component("server_shop.balance"))
                .setLore(lang.component("server_shop.balance_lore", "amount",
                        currency.format(wallet.balance(player.getUniqueId(), currency.id()))))
                .build());
        setItem(CLOSE, ItemBuilder.createCancelButton(lang.raw("server_shop.close")));
    }

    private ItemStack icon(ServerShopCategory category) {

        Material material = Material.matchMaterial(category.icon());
        List<Component> lore = new ArrayList<>();
        category.description().forEach(line -> lore.add(ComponentUtils.parse(line)));
        lore.add(Component.empty());
        lore.add(lang.component("server_shop.category_count", "count", category.entries().size()));
        lore.add(lang.component(service.canEnter(player, category) ? "server_shop.category_open"
                : "server_shop.category_locked"));

        ItemStack item = new ItemBuilder(material == null || !material.isItem() ? Material.CHEST : material)
                .setName(ComponentUtils.parse(category.displayName()))
                .setLore(lore)
                .build();

        if (category.premium()) {
            ItemMeta meta = item.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == CLOSE) {
            player.closeInventory();
            return;
        }

        ServerShopCategory category = bySlot.get(slot);
        if (category == null) {
            return;
        }

        if (!service.canEnter(player, category)) {
            lang.send(player, "server_shop.no_permission");
            return;
        }

        new ServerShopCategoryGUI(player, category, service, wallet, lang,
                () -> new ServerShopGUI(player, shops, service, wallet, lang).open()).open();
    }

}
