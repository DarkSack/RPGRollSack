package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.servershop.ServerShopCategory;
import com.sack.rpgroll.economy.servershop.ServerShopEntry;
import com.sack.rpgroll.economy.servershop.ServerShopItems;
import com.sack.rpgroll.economy.servershop.ServerShopService;
import com.sack.rpgroll.economy.wallet.WalletService;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Una sección de la tienda. Clic izquierdo compra un lote y con shift una pila;
 * clic derecho vende un lote y con shift todo lo que se lleve encima.
 */
public class ServerShopCategoryGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;
    private static final int BACK = 45;
    private static final int PREVIOUS = 48;
    private static final int BALANCE = 49;
    private static final int NEXT = 50;
    private static final int CLOSE = 53;

    private final ServerShopCategory category;
    private final ServerShopService service;
    private final WalletService wallet;
    private final LangManager lang;
    private final Runnable onBack;
    private int page;

    public ServerShopCategoryGUI(Player player, ServerShopCategory category, ServerShopService service,
            WalletService wallet, LangManager lang, Runnable onBack) {
        super(player, ComponentUtils.parse(category.displayName()), SIZE);
        this.category = category;
        this.service = service;
        this.wallet = wallet;
        this.lang = lang;
        this.onBack = onBack;
    }

    private int pages() {
        return Math.max(1, (category.entries().size() + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    public void build() {

        clear();
        page = Math.max(0, Math.min(page, pages() - 1));

        ItemStack filler = category.premium()
                ? new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setName(Component.empty()).build()
                : ItemBuilder.createFiller();

        for (int slot = PER_PAGE; slot < SIZE; slot++) {
            setItem(slot, filler);
        }

        Currency currency = service.currency(category);
        List<ServerShopEntry> entries = category.entries();

        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= entries.size()) {
                break;
            }
            setItem(i, icon(entries.get(index), currency));
        }

        setItem(BACK, new ItemBuilder(Material.ARROW).setName(lang.component("server_shop.back")).build());
        if (page > 0) {
            setItem(PREVIOUS, new ItemBuilder(Material.ARROW).setName(lang.component("server_shop.previous")).build());
        }
        if (page < pages() - 1) {
            setItem(NEXT, new ItemBuilder(Material.ARROW).setName(lang.component("server_shop.next")).build());
        }
        setItem(BALANCE, new ItemBuilder(Material.SUNFLOWER)
                .setName(lang.component("server_shop.balance"))
                .setLore(lang.component("server_shop.balance_lore", "amount",
                        currency.format(wallet.balance(player.getUniqueId(), currency.id()))),
                        lang.component("server_shop.page", "page", page + 1, "pages", pages()))
                .build());
        setItem(CLOSE, ItemBuilder.createCancelButton(lang.raw("server_shop.close")));
    }

    private ItemStack icon(ServerShopEntry entry, Currency currency) {

        Optional<ItemStack> created = ServerShopItems.create(entry);

        if (created.isEmpty()) {
            String plugin = ServerShopItems.requiredPlugin(entry);
            return new ItemBuilder(Material.BARRIER)
                    .setName(Component.text(entry.key()))
                    .setLore(lang.component(plugin == null ? "server_shop.unknown_id" : "server_shop.missing_plugin",
                            "plugin", plugin == null ? "" : plugin, "id", entry.key()))
                    .build();
        }

        ItemStack item = created.get();
        item.setAmount(Math.min(entry.amount(), item.getMaxStackSize()));
        ItemMeta meta = item.getItemMeta();

        if (entry.name() != null) {
            meta.displayName(ComponentUtils.parse(entry.name()));
        }

        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        entry.lore().forEach(line -> lore.add(ComponentUtils.parse(line)));
        lore.add(Component.empty());

        double buy = service.buyPrice(entry, player.getLocation());
        double sell = service.sellPrice(entry, player.getLocation());

        if (buy > 0) {
            lore.add(lang.component("server_shop.buy_line", "amount", entry.amount(), "price", currency.format(buy)));
        }
        if (sell > 0) {
            lore.add(lang.component("server_shop.sell_line", "amount", entry.amount(), "price", currency.format(sell)));
        }
        if (entry.market() != null) {
            lore.add(lang.component("server_shop.market_hint"));
        }
        if (buy > 0) {
            lore.add(lang.component("server_shop.buy_hint", "bulk", bulkLots(entry, item) * entry.amount()));
        }
        if (sell > 0) {
            lore.add(lang.component("server_shop.sell_hint"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Cuántos lotes compra un shift+clic: una pila entera, o uno si el lote ya la llena. */
    private static int bulkLots(ServerShopEntry entry, ItemStack unit) {
        return Math.max(1, unit.getMaxStackSize() / entry.amount());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case BACK -> {
                onBack.run();
                return;
            }
            case PREVIOUS -> {
                page--;
                build();
                return;
            }
            case NEXT -> {
                page++;
                build();
                return;
            }
            case CLOSE -> {
                player.closeInventory();
                return;
            }
            default -> {
            }
        }

        int index = page * PER_PAGE + slot;
        if (slot < 0 || slot >= PER_PAGE || index >= category.entries().size()) {
            return;
        }

        ServerShopEntry entry = category.entries().get(index);
        ClickType click = event.getClick();
        ServerShopService.Outcome outcome;

        if (click.isRightClick()) {
            outcome = service.sell(player, category, entry, click.isShiftClick());
        } else if (click.isLeftClick()) {
            int lots = click.isShiftClick()
                    ? ServerShopItems.create(entry).map(unit -> bulkLots(entry, unit)).orElse(1) : 1;
            outcome = service.buy(player, category, entry, lots);
        } else {
            return;
        }

        report(entry, outcome);
        build();
    }

    private void report(ServerShopEntry entry, ServerShopService.Outcome outcome) {

        String item = displayName(entry);
        String price = service.currency(category).format(outcome.money());

        switch (outcome.result()) {
            case BOUGHT -> {
                lang.send(player, "server_shop.bought", "amount", outcome.units(), "item", item, "price", price);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
            }
            case SOLD -> {
                lang.send(player, "server_shop.sold", "amount", outcome.units(), "item", item, "price", price);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.4f);
            }
            case INSUFFICIENT_FUNDS -> {
                lang.send(player, "server_shop.insufficient_funds");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            }
            case NOTHING_TO_SELL -> lang.send(player, "server_shop.nothing_to_sell", "amount", entry.amount(),
                    "item", item);
            default -> lang.send(player, "server_shop." + outcome.result().name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private String displayName(ServerShopEntry entry) {

        if (entry.name() != null) {
            return PlainTextComponentSerializer.plainText().serialize(ComponentUtils.parse(entry.name()));
        }

        return ServerShopItems.create(entry)
                .map(stack -> PlainTextComponentSerializer.plainText().serialize(stack.effectiveName()))
                .orElse(entry.key());
    }

}
