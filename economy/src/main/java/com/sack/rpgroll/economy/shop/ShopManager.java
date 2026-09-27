package com.sack.rpgroll.economy.shop;

import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.servershop.InventorySpace;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.economy.tax.TaxResult;
import com.sack.rpgroll.economy.tax.TaxType;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Registro en memoria de las tiendas de todos los jugadores + la lógica de compra. */
public class ShopManager {

    /** Líneas que caben en la GUI de una tienda (4 filas de 9). */
    public static final int MAX_LISTINGS = 36;

    private final ShopStore store;
    private final WalletService walletService;
    private final TaxEngine taxEngine;
    private final Map<UUID, PlayerShop> shops = new ConcurrentHashMap<>();
    private Function<ItemStack, String> blockReason = item -> null;

    public ShopManager(ShopStore store, WalletService walletService, TaxEngine taxEngine) {
        this.store = store;
        this.walletService = walletService;
        this.taxEngine = taxEngine;
    }

    /** Qué ítems no se pueden vender: devuelve la clave del mensaje de rechazo, o null. */
    public void blockReason(Function<ItemStack, String> blockReason) {
        this.blockReason = blockReason;
    }

    /** null si el ítem se puede poner a la venta; si no, la clave del mensaje de rechazo. */
    public String blockReason(ItemStack item) {
        return blockReason.apply(item);
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

    // ---------------------------------------------------------------- stock

    /**
     * Mete en la tienda el stack que entrega su dueño (el llamador ya se lo
     * quitó del inventario): si ya vende ese mismo ítem se suma a su stock y
     * se actualiza el precio; si no, es una línea nueva.
     *
     * @return la línea, o vacío si la tienda ya tiene {@link #MAX_LISTINGS} líneas
     */
    public Optional<ShopListing> addStock(PlayerShop shop, ItemStack stack, double unitPrice, String displayName) {

        for (ShopListing listing : shop.listings()) {
            if (!listing.isUnlimited() && listing.item().isSimilar(stack)) {
                listing.addStock(stack.getAmount());
                listing.setUnitPrice(unitPrice);
                store.save(shop);
                return Optional.of(listing);
            }
        }

        if (shop.listings().size() >= MAX_LISTINGS) {
            return Optional.empty();
        }

        ShopListing listing = new ShopListing(stack, displayName, unitPrice, stack.getAmount());
        shop.listings().add(listing);
        store.save(shop);
        return Optional.of(listing);
    }

    /**
     * Devuelve a su dueño lo que queda sin vender de una línea y, si cabe
     * todo, la quita. Lo que no quepa en el inventario se queda en la tienda.
     *
     * @return unidades que siguen en la tienda (0 = la línea se quitó)
     */
    public int withdrawListing(PlayerShop shop, ShopListing listing, Player owner) {

        if (!shop.listings().contains(listing)) {
            return 0;
        }

        if (listing.isUnlimited()) {
            shop.listings().remove(listing);
            store.save(shop);
            return 0;
        }

        ItemStack unit = listing.item();
        int returned = Math.min(listing.stock(), InventorySpace.room(owner.getInventory().getStorageContents(), unit));

        give(owner.getInventory(), unit, returned);
        listing.reduceStock(returned);

        if (listing.stock() <= 0) {
            shop.listings().remove(listing);
        }

        store.save(shop);
        return listing.stock();
    }

    // ---------------------------------------------------------------- comprar

    public ShopPurchaseResult buy(Player buyer, PlayerShop shop, ShopListing listing, int quantity) {

        if (!shop.isOpen()) {
            return ShopPurchaseResult.SHOP_CLOSED;
        }

        // La línea pudo quitarse mientras el comprador tenía la tienda abierta.
        if (quantity <= 0 || !shop.listings().contains(listing)) {
            return ShopPurchaseResult.OUT_OF_STOCK;
        }

        if (!listing.isUnlimited() && listing.stock() < quantity) {
            return ShopPurchaseResult.OUT_OF_STOCK;
        }

        double grossTotal = listing.unitPrice() * quantity;

        if (!Double.isFinite(grossTotal) || grossTotal < 0) {
            return ShopPurchaseResult.OUT_OF_STOCK;
        }

        ItemStack unit = listing.item();
        PlayerInventory inventory = buyer.getInventory();

        // Antes bastaba un hueco libre y lo que no cabía de un shift-click se perdía, ya pagado.
        if (InventorySpace.room(inventory.getStorageContents(), unit) < quantity) {
            return ShopPurchaseResult.INVENTORY_FULL;
        }

        if (grossTotal > 0) {

            // El resultado se ignoraba: una cartera bloqueada compraba gratis.
            EconomyResult paid = walletService.withdraw(buyer.getUniqueId(), shop.currencyId(), grossTotal,
                    TransactionType.SHOP_PURCHASE, "Compra en tienda de " + shop.name());

            if (paid != EconomyResult.SUCCESS) {
                return ShopPurchaseResult.INSUFFICIENT_FUNDS;
            }

            if (!shop.isServerShop()) {
                TaxResult tax = taxEngine.apply(TaxType.SALE, listing.material().name(), grossTotal, shop.ownerId(),
                        shop.currencyId());

                // Si el dueño no puede recibirlo (cartera bloqueada o tope) el ítem se entrega igual: el
                // comprador ya pagó, y el intento queda en el libro mayor.
                walletService.deposit(shop.ownerId(), shop.currencyId(), tax.netAmount(), TransactionType.SHOP_SALE,
                        "Venta en tienda " + shop.name() + " a " + buyer.getName());
            }
            // En una tienda del servidor no se le paga a nadie: el dinero sale de la economía.
        }

        listing.reduceStock(quantity);
        give(inventory, unit, quantity);
        store.save(shop);

        return ShopPurchaseResult.SUCCESS;
    }

    private static void give(PlayerInventory inventory, ItemStack unit, int units) {

        int max = unit.getMaxStackSize();
        int left = units;

        while (left > 0) {
            ItemStack stack = unit.clone();
            stack.setAmount(Math.min(max, left));
            inventory.addItem(stack);
            left -= stack.getAmount();
        }
    }

}
