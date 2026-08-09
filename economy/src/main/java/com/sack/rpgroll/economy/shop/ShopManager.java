package com.sack.rpgroll.economy.shop;

import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.economy.tax.TaxResult;
import com.sack.rpgroll.economy.tax.TaxType;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Registro en memoria de las tiendas de todos los jugadores + la lógica de compra. */
public class ShopManager {

    private final ShopStore store;
    private final WalletService walletService;
    private final TaxEngine taxEngine;
    private final Map<UUID, PlayerShop> shops = new ConcurrentHashMap<>();

    public ShopManager(ShopStore store, WalletService walletService, TaxEngine taxEngine) {
        this.store = store;
        this.walletService = walletService;
        this.taxEngine = taxEngine;
    }

    public void loadAll() {
        shops.clear();
        for (PlayerShop shop : store.loadAll()) {
            shops.put(shop.id(), shop);
        }
    }

    public void saveAll() {
        shops.values().forEach(store::save);
    }

    public PlayerShop create(UUID ownerId, String name, String currencyId) {
        PlayerShop shop = new PlayerShop(UUID.randomUUID(), ownerId, name, currencyId);
        shops.put(shop.id(), shop);
        store.save(shop);
        return shop;
    }

    public void save(PlayerShop shop) {
        store.save(shop);
    }

    public void delete(UUID id) {
        shops.remove(id);
        store.delete(id);
    }

    public Optional<PlayerShop> get(UUID id) {
        return Optional.ofNullable(shops.get(id));
    }

    public List<PlayerShop> byOwner(UUID ownerId) {
        List<PlayerShop> result = new ArrayList<>();
        for (PlayerShop shop : shops.values()) {
            if (shop.ownerId().equals(ownerId)) {
                result.add(shop);
            }
        }
        return result;
    }

    public java.util.Collection<PlayerShop> all() {
        return shops.values();
    }

    public ShopPurchaseResult buy(Player buyer, PlayerShop shop, ShopListing listing, int quantity) {

        if (!shop.isOpen()) {
            return ShopPurchaseResult.SHOP_CLOSED;
        }

        if (!listing.isUnlimited() && listing.stock() < quantity) {
            return ShopPurchaseResult.OUT_OF_STOCK;
        }

        double grossTotal = listing.unitPrice() * quantity;

        if (!walletService.has(buyer.getUniqueId(), shop.currencyId(), grossTotal)) {
            return ShopPurchaseResult.INSUFFICIENT_FUNDS;
        }

        ItemStack item = new ItemStack(listing.material(), quantity);
        if (buyer.getInventory().firstEmpty() == -1) {
            return ShopPurchaseResult.INVENTORY_FULL;
        }

        walletService.withdraw(buyer.getUniqueId(), shop.currencyId(), grossTotal, TransactionType.SHOP_PURCHASE,
                "Compra en tienda de " + shop.name());

        TaxResult tax = taxEngine.apply(TaxType.SALE, listing.material().name(), grossTotal, shop.ownerId(),
                shop.currencyId());

        EconomyResult payout = walletService.deposit(shop.ownerId(), shop.currencyId(), tax.netAmount(),
                TransactionType.SHOP_SALE, "Venta en tienda " + shop.name() + " a " + buyer.getName());

        if (payout != EconomyResult.SUCCESS) {
            // El dueño no puede recibir el pago (wallet bloqueado/tope) — igual se entrega el ítem,
            // el monto queda documentado en el libro mayor como intento fallido.
        }

        listing.reduceStock(quantity);
        buyer.getInventory().addItem(item);
        store.save(shop);

        return ShopPurchaseResult.SUCCESS;
    }

}
