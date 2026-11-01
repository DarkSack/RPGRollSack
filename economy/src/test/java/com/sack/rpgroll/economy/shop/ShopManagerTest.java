package com.sack.rpgroll.economy.shop;

import com.sack.rpgroll.economy.tax.*;
import com.sack.rpgroll.economy.wallet.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopManagerTest {
    @Test
    void rejectedSellerPaymentReturnsMoneyAndKeepsStockWithoutDeliveryOrTax() {
        WalletService wallet = mock(WalletService.class);
        TaxEngine tax = mock(TaxEngine.class);
        ShopManager manager = new ShopManager(mock(ShopStore.class), wallet, tax);
        Player buyer = mock(Player.class);
        UUID buyerId = UUID.randomUUID(), sellerId = UUID.randomUUID();
        when(buyer.getUniqueId()).thenReturn(buyerId);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(buyer.getInventory()).thenReturn(inventory);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
        ItemStack unit = mock(ItemStack.class);
        when(unit.asOne()).thenReturn(unit);
        when(unit.clone()).thenReturn(unit);
        when(unit.getType()).thenReturn(Material.PAPER);
        when(unit.getMaxStackSize()).thenReturn(64);
        PlayerShop shop = manager.create(sellerId, "Shop", "gold");
        ShopListing listing = new ShopListing(unit, "Paper", 100, 5);
        shop.listings().add(listing);
        when(wallet.withdraw(eq(buyerId), anyString(), anyDouble(), any(), anyString())).thenReturn(EconomyResult.SUCCESS);
        when(wallet.deposit(eq(sellerId), anyString(), anyDouble(), any(), anyString())).thenReturn(EconomyResult.LIMIT_EXCEEDED);
        when(wallet.deposit(eq(buyerId), anyString(), anyDouble(), any(), anyString())).thenReturn(EconomyResult.SUCCESS);
        when(tax.quote(TaxType.SALE, "PAPER", 100)).thenReturn(TaxResult.none(100));
        assertEquals(ShopPurchaseResult.SELLER_UNAVAILABLE, manager.buy(buyer, shop, listing, 1));
        assertEquals(5, listing.stock());
        verify(inventory, never()).addItem(any(ItemStack.class));
        verify(wallet).deposit(eq(buyerId), eq("gold"), eq(100.0), any(), anyString());
        verify(tax, never()).record(any(), anyString(), any(), anyString(), any());
    }
}
