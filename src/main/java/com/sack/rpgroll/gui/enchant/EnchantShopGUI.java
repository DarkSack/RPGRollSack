package com.sack.rpgroll.gui.enchant;

import com.sack.rpgroll.gameplay.enchant.CustomEnchantment;
import com.sack.rpgroll.gameplay.enchant.EnchantManager;
import com.sack.rpgroll.gameplay.enchant.EnchantedBookFactory;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.integration.VaultEconomyProvider;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tienda de libros encantados custom, vendidos por Bibliotecarios.
 * Solo vende nivel 1 de cada encantamiento marcado como vendible —
 * subir de nivel se hace combinando en yunque (EnchantCombiner).
 */
public class EnchantShopGUI extends InventoryGUI {

    private static final int[] CONTENT_SLOTS = { 10, 11, 12, 13, 14, 15, 16 };
    private static final int SELL_LEVEL = 1;

    private final EnchantManager enchantManager;
    private final EnchantedBookFactory bookFactory;
    private final VaultEconomyProvider economyProvider;
    private final Map<Integer, CustomEnchantment> slotToEnchant;

    public EnchantShopGUI(Player player, EnchantManager enchantManager, EnchantedBookFactory bookFactory,
            VaultEconomyProvider economyProvider) {
        super(player, Component.text("Tienda del Bibliotecario", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                27);
        this.enchantManager = enchantManager;
        this.bookFactory = bookFactory;
        this.economyProvider = economyProvider;
        this.slotToEnchant = new HashMap<>();
    }

    @Override
    public void build() {

        clear();
        slotToEnchant.clear();

        for (int i = 0; i < 9; i++) {
            setItem(i, ItemBuilder.createFiller());
        }
        for (int i = 18; i < 27; i++) {
            setItem(i, ItemBuilder.createFiller());
        }

        if (economyProvider.getEconomy().isEmpty()) {
            setItem(13, new ItemBuilder(Material.BARRIER)
                    .setName(Component.text("Tienda no disponible", NamedTextColor.RED))
                    .setLore(Component.text("No hay economía activa en el servidor.", NamedTextColor.GRAY))
                    .build());
            return;
        }

        List<CustomEnchantment> sellable = enchantManager.getAll().stream()
                .filter(CustomEnchantment::isSellable)
                .toList();

        for (int i = 0; i < sellable.size() && i < CONTENT_SLOTS.length; i++) {
            addOffer(CONTENT_SLOTS[i], sellable.get(i));
        }
    }

    private void addOffer(int slot, CustomEnchantment enchant) {

        ItemStack book = bookFactory.create(enchant, SELL_LEVEL);
        ItemStack display = book.clone();

        List<Component> lore = new ArrayList<>(display.getItemMeta().hasLore()
                ? display.getItemMeta().lore()
                : List.of());

        double price = enchant.getShopPrice(SELL_LEVEL);
        lore.add(Component.text(""));
        lore.add(Component.text("Precio: $" + price, NamedTextColor.GREEN));
        lore.add(Component.text("Click para comprar", NamedTextColor.YELLOW));

        var meta = display.getItemMeta();
        meta.lore(lore);
        display.setItemMeta(meta);

        setItem(slot, display);
        slotToEnchant.put(slot, enchant);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        CustomEnchantment enchant = slotToEnchant.get(event.getRawSlot());

        if (enchant == null) {
            return;
        }

        economyProvider.getEconomy().ifPresentOrElse(economy -> {

            double price = enchant.getShopPrice(SELL_LEVEL);

            if (economy.getBalance(player) < price) {
                player.sendMessage(Component.text("No tienes suficiente dinero.", NamedTextColor.RED));
                return;
            }

            economy.withdrawPlayer(player, price);

            ItemStack book = bookFactory.create(enchant, SELL_LEVEL);
            var leftover = player.getInventory().addItem(book);

            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

            player.sendMessage(Component.text("Compraste: ", NamedTextColor.GREEN)
                    .append(Component.text("Libro: " + enchant.displayName(), NamedTextColor.GOLD)));

        }, () -> player.sendMessage(Component.text("La tienda no está disponible.", NamedTextColor.RED)));
    }

}