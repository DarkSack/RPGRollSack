package com.sack.rpgroll.gameplay.enchant.listener;

import com.sack.rpgroll.gameplay.enchant.EnchantManager;
import com.sack.rpgroll.gameplay.enchant.EnchantedBookFactory;
import com.sack.rpgroll.gui.enchant.EnchantShopGUI;
import com.sack.rpgroll.integration.VaultEconomyProvider;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Convierte a los Bibliotecarios en vendedores de libros encantados custom.
 * Cancela el trade vanilla y abre EnchantShopGUI en su lugar.
 */
public class EnchantShopListener implements Listener {

    private final EnchantManager enchantManager;
    private final EnchantedBookFactory bookFactory;
    private final VaultEconomyProvider economyProvider;

    public EnchantShopListener(EnchantManager enchantManager, EnchantedBookFactory bookFactory,
            VaultEconomyProvider economyProvider) {
        this.enchantManager = enchantManager;
        this.bookFactory = bookFactory;
        this.economyProvider = economyProvider;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }

        if (villager.getProfession() != Villager.Profession.LIBRARIAN) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        new EnchantShopGUI(player, enchantManager, bookFactory, economyProvider).open();
    }

}