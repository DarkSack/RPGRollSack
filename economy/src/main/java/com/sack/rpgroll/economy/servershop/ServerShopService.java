package com.sack.rpgroll.economy.servershop;

import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.market.MarketEngine;
import com.sack.rpgroll.economy.market.MarketProduct;
import com.sack.rpgroll.economy.market.MarketProductManager;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;

/**
 * Compra y venta contra la tienda del servidor. Los precios son los del YAML
 * o, si la línea está enlazada a un producto del mercado, los del mercado en
 * ese momento: comprar sube su demanda y vender su oferta.
 */
public class ServerShopService {

    public enum Result {
        BOUGHT, SOLD, NO_PERMISSION, NOT_FOR_SALE, NOT_BOUGHT, UNAVAILABLE, INSUFFICIENT_FUNDS, INVENTORY_FULL,
        NOTHING_TO_SELL, LOCKED, UNKNOWN_CURRENCY
    }

    /** Qué pasó, cuántas unidades se movieron y por cuánto dinero. */
    public record Outcome(Result result, int units, double money) {

        static Outcome of(Result result) {
            return new Outcome(result, 0, 0);
        }

    }

    private final WalletService wallet;
    private final CurrencyManager currencies;
    private final MarketProductManager products;
    private final MarketEngine market;
    private double sellRatio;

    public ServerShopService(WalletService wallet, CurrencyManager currencies, MarketProductManager products,
            MarketEngine market, double sellRatio) {
        this.wallet = wallet;
        this.currencies = currencies;
        this.products = products;
        this.market = market;
        setSellRatio(sellRatio);
    }

    /** Qué fracción del precio de mercado se le paga a quien vende una línea enlazada al mercado. */
    public void setSellRatio(double sellRatio) {
        this.sellRatio = Math.max(0, Math.min(1, sellRatio));
    }

    /** La moneda de la sección; con {@code null}, la moneda por defecto. */
    public Currency currency(ServerShopCategory category) {
        return category == null || category.currency() == null ? currencies.defaultCurrency()
                : currencies.get(category.currency()).orElse(currencies.defaultCurrency());
    }

    public boolean canEnter(Player player, ServerShopCategory category) {
        return category.permission() == null || player.hasPermission(category.permission());
    }

    /** Lo que cuesta un lote ({@code amount} unidades). 0 si no se vende. */
    public double buyPrice(ServerShopEntry entry, Location location) {
        return marketProduct(entry)
                .map(product -> round(market.price(product, location) * entry.amount()))
                .orElse(entry.buy());
    }

    /** Lo que se paga por un lote. 0 si la tienda no lo compra. */
    public double sellPrice(ServerShopEntry entry, Location location) {

        if (!entry.sellable()) {
            return 0;
        }

        return marketProduct(entry)
                .map(product -> round(market.price(product, location) * entry.amount() * sellRatio))
                .orElse(entry.sell());
    }

    public Outcome buy(Player player, ServerShopCategory category, ServerShopEntry entry, int lots) {

        if (!canEnter(player, category)) {
            return Outcome.of(Result.NO_PERMISSION);
        }

        double price = round(buyPrice(entry, player.getLocation()) * Math.max(1, lots));
        if (price <= 0) {
            return Outcome.of(Result.NOT_FOR_SALE);
        }

        Optional<ItemStack> unit = ServerShopItems.create(entry);
        if (unit.isEmpty()) {
            return Outcome.of(Result.UNAVAILABLE);
        }

        int units = entry.amount() * Math.max(1, lots);
        PlayerInventory inventory = player.getInventory();

        if (InventorySpace.room(inventory.getStorageContents(), unit.get()) < units) {
            return Outcome.of(Result.INVENTORY_FULL);
        }

        EconomyResult paid = wallet.withdraw(player.getUniqueId(), currency(category).id(), price,
                TransactionType.MARKET_BUY, "Tienda del servidor: " + entry.key() + " x" + units);

        if (paid != EconomyResult.SUCCESS) {
            return Outcome.of(failure(paid));
        }

        give(inventory, unit.get(), units);

        if (marketProduct(entry).isPresent()) {
            market.recordBuy(entry.market(), units);
        }

        return new Outcome(Result.BOUGHT, units, price);
    }

    /** Vende un lote, o todos los lotes completos que lleve encima si {@code all}. */
    public Outcome sell(Player player, ServerShopCategory category, ServerShopEntry entry, boolean all) {

        if (!canEnter(player, category)) {
            return Outcome.of(Result.NO_PERMISSION);
        }

        double lotPrice = sellPrice(entry, player.getLocation());
        if (lotPrice <= 0) {
            return Outcome.of(Result.NOT_BOUGHT);
        }

        // Solo lo vanilla "limpio": un ítem con nombre, encantado o de RPGRoll no cuenta como su material.
        ItemStack template = new ItemStack(Material.valueOf(entry.key()));
        PlayerInventory inventory = player.getInventory();
        int have = InventorySpace.count(inventory.getStorageContents(), template);
        int lots = all ? have / entry.amount() : have >= entry.amount() ? 1 : 0;

        if (lots == 0) {
            return Outcome.of(Result.NOTHING_TO_SELL);
        }

        int units = lots * entry.amount();
        double money = round(lotPrice * lots);

        take(inventory, template, units);

        EconomyResult paid = wallet.deposit(player.getUniqueId(), currency(category).id(), money,
                TransactionType.MARKET_SELL, "Tienda del servidor: " + entry.key() + " x" + units);

        if (paid != EconomyResult.SUCCESS) {
            // Sin cobro no hay venta: se le devuelve lo que se le quitó.
            give(inventory, template, units);
            return Outcome.of(failure(paid));
        }

        if (marketProduct(entry).isPresent()) {
            market.recordSell(entry.market(), units);
        }

        return new Outcome(Result.SOLD, units, money);
    }

    private Optional<MarketProduct> marketProduct(ServerShopEntry entry) {
        return entry.market() == null ? Optional.empty() : products.get(entry.market());
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

    private static void take(PlayerInventory inventory, ItemStack template, int units) {

        int max = template.getMaxStackSize();
        int left = units;

        while (left > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(max, left));
            inventory.removeItem(stack);
            left -= stack.getAmount();
        }
    }

    private static Result failure(EconomyResult result) {

        return switch (result) {
            case INSUFFICIENT_FUNDS -> Result.INSUFFICIENT_FUNDS;
            case LOCKED -> Result.LOCKED;
            default -> Result.UNKNOWN_CURRENCY;
        };
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

}
