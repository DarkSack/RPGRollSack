package com.sack.rpgroll.economy.buyer;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * La ventana del comprador: arriba (45 casillas) el jugador deja lo que quiere vender; abajo, la
 * barra con el total, "Vender" y "Cancelar". Lo que no se vende vuelve siempre al jugador al cerrar.
 */
public final class BuyerMenu implements InventoryHolder {

    static final int SIZE = 54;
    static final int DEPOSIT_SLOTS = 45;
    static final int CANCEL = 45;
    static final int SELL = 49;
    static final int HELP = 53;

    private final Player player;
    private final BuyerService service;
    private final LangManager lang;
    private final Inventory inventory;
    private boolean closed;

    public BuyerMenu(Player player, BuyerService service, LangManager lang) {
        this.player = player;
        this.service = service;
        this.lang = lang;
        this.inventory = Bukkit.createInventory(this, SIZE, lang.component("buyer.title"));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {

        ItemStack filler = ItemBuilder.createFiller();

        for (int slot = DEPOSIT_SLOTS; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(CANCEL, new ItemBuilder(Material.BARRIER).setName(lang.component("buyer.cancel_name")).build());
        inventory.setItem(HELP, new ItemBuilder(Material.BOOK).setName(lang.component("buyer.help_name"))
                .setLore(ItemBuilder.toLoreLines(lang.raw("buyer.help_lore"))).build());
        refresh();
        player.openInventory(inventory);
    }

    private ItemStack[] deposited() {
        ItemStack[] items = new ItemStack[DEPOSIT_SLOTS];
        for (int slot = 0; slot < DEPOSIT_SLOTS; slot++) {
            items[slot] = inventory.getItem(slot);
        }
        return items;
    }

    /** Vuelve a calcular el total que muestra el botón de vender. */
    public void refresh() {

        BuyerService.Quote quote = service.quote(player, deposited());
        List<Component> lore = new ArrayList<>();

        if (quote.lines().isEmpty()) {
            lore.add(lang.component("buyer.sell_empty"));
        } else {
            lore.add(lang.component("buyer.sell_count", "count", quote.acceptedItems()));
            quote.totals().forEach((currency, money) -> lore.add(lang.component("buyer.sell_total", "money",
                    currency.format(money))));
        }

        if (!quote.rejectedSlots().isEmpty()) {
            lore.add(lang.component("buyer.sell_rejected", "count", quote.rejectedSlots().size()));
        }

        inventory.setItem(SELL, new ItemBuilder(quote.lines().isEmpty() ? Material.GRAY_DYE : Material.EMERALD)
                .setName(lang.component("buyer.sell_name")).setLore(lore).build());
    }

    public void sell() {

        BuyerService.Quote quote = service.quote(player, deposited());

        if (quote.lines().isEmpty()) {
            lang.send(player, "buyer.nothing");
            return;
        }

        List<BuyerService.Line> paid = service.pay(player, quote);

        if (paid.isEmpty()) {
            lang.send(player, "buyer.failed");
            return;
        }

        for (BuyerService.Line line : paid) {
            inventory.setItem(line.slot(), null);
        }

        int count = paid.stream().mapToInt(line -> line.item().getAmount()).sum();
        String money = paid.stream()
                .collect(Collectors.groupingBy(BuyerService.Line::currency,
                        Collectors.summingDouble(BuyerService.Line::money)))
                .entrySet().stream().map(e -> formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(" + "));

        lang.send(player, "buyer.sold", "count", count, "money", money);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        refresh();
    }

    private static String formatted(Currency currency, double money) {
        return currency.format(money);
    }

    /** Al cerrar: todo lo que quede arriba vuelve al jugador (lo que no cabe cae a sus pies). */
    public void returnItems() {

        if (closed) {
            return;
        }
        closed = true;

        int returned = 0;

        for (int slot = 0; slot < DEPOSIT_SLOTS; slot++) {

            ItemStack item = inventory.getItem(slot);

            if (item == null || item.getType().isAir()) {
                continue;
            }

            inventory.setItem(slot, null);
            returned++;
            player.getInventory().addItem(item).values()
                    .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        }

        if (returned > 0) {
            lang.send(player, "buyer.returned", "count", returned);
        }
    }

}
