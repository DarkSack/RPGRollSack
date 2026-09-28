package com.sack.rpgroll.economy.buyer;

import com.sack.rpgroll.common.item.SellValue;
import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.servershop.ServerShopCategory;
import com.sack.rpgroll.economy.servershop.ServerShopEntry;
import com.sack.rpgroll.economy.servershop.ServerShopManager;
import com.sack.rpgroll.economy.servershop.ServerShopService;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * El comprador: le pagas cualquier cosa que tenga precio. Primero cuenta el valor que el propio ítem
 * trae ({@link SellValue}: peces, productos del rancho...); si no trae, lo que paga /tienda por ese
 * material "limpio" (sin nombre, encantamientos ni datos de plugins), mercado dinámico incluido.
 * Lo demás no lo compra.
 */
public class BuyerService {

    /** Un ítem que sí compra: cuánto paga por él, en qué moneda y (si viene de /tienda) de qué línea. */
    public record Line(int slot, ItemStack item, double money, Currency currency, ServerShopEntry entry) {
    }

    /** Lo que pagaría por un montón de ítems. */
    public record Quote(List<Line> lines, List<Integer> rejectedSlots) {

        public Map<Currency, Double> totals() {
            Map<Currency, Double> totals = new LinkedHashMap<>();
            lines.forEach(line -> totals.merge(line.currency(), line.money(), Double::sum));
            return totals;
        }

        public int acceptedItems() {
            return lines.stream().mapToInt(line -> line.item().getAmount()).sum();
        }
    }

    private final ServerShopManager shopManager;
    private final ServerShopService shopService;
    private final CurrencyManager currencies;
    private final WalletService wallet;
    private double valueMultiplier = 1.0;
    private boolean acceptShopPrices = true;

    public BuyerService(ServerShopManager shopManager, ServerShopService shopService, CurrencyManager currencies,
            WalletService wallet) {
        this.shopManager = shopManager;
        this.shopService = shopService;
        this.currencies = currencies;
        this.wallet = wallet;
    }

    /** buyer.value-multiplier y buyer.accept-shop-prices del config.yml. */
    public void configure(double valueMultiplier, boolean acceptShopPrices) {
        this.valueMultiplier = Math.max(0, valueMultiplier);
        this.acceptShopPrices = acceptShopPrices;
    }

    /** @param items por casilla (null = vacía) */
    public Quote quote(Player player, ItemStack[] items) {

        List<Line> lines = new ArrayList<>();
        List<Integer> rejected = new ArrayList<>();
        Map<Material, Offer> shopOffers = acceptShopPrices ? shopOffers(player) : Map.of();

        for (int slot = 0; slot < items.length; slot++) {

            ItemStack item = items[slot];

            if (item == null || item.getType().isAir()) {
                continue;
            }

            Line line = price(slot, item, shopOffers, player);

            if (line == null) {
                rejected.add(slot);
            } else {
                lines.add(line);
            }
        }

        return new Quote(lines, rejected);
    }

    private Line price(int slot, ItemStack item, Map<Material, Offer> shopOffers, Player player) {

        OptionalDouble own = SellValue.get(item);

        if (own.isPresent()) {
            return new Line(slot, item, round(own.getAsDouble() * item.getAmount() * valueMultiplier),
                    currencies.defaultCurrency(), null);
        }

        Offer offer = shopOffers.get(item.getType());

        // Solo lo vanilla "limpio": un ítem con nombre, encantado o de otro plugin no vale como su material.
        if (offer == null || !item.isSimilar(new ItemStack(item.getType()))) {
            return null;
        }

        double lotPrice = shopService.sellPrice(offer.entry(), player.getLocation());
        double unit = lotPrice / offer.entry().amount();

        if (unit <= 0) {
            return null;
        }

        return new Line(slot, item, round(unit * item.getAmount() * valueMultiplier),
                shopService.currency(offer.category()), offer.entry());
    }

    /**
     * Cobra lo que se vendió: por cada moneda, un depósito. Si uno falla, esas líneas no se venden.
     *
     * @return las líneas que de verdad se pagaron (el que llama quita esos ítems)
     */
    public List<Line> pay(Player player, Quote quote) {

        List<Line> paid = new ArrayList<>();

        for (var entry : quote.totals().entrySet()) {

            Currency currency = entry.getKey();
            List<Line> ofCurrency = quote.lines().stream().filter(line -> line.currency().equals(currency)).toList();
            int units = ofCurrency.stream().mapToInt(line -> line.item().getAmount()).sum();

            EconomyResult result = wallet.deposit(player.getUniqueId(), currency.id(), round(entry.getValue()),
                    TransactionType.MARKET_SELL, "Comprador: " + units + " ítem(s)");

            if (result != EconomyResult.SUCCESS) {
                continue;
            }

            for (Line line : ofCurrency) {
                if (line.entry() != null) {
                    shopService.recordMarketSell(line.entry(), line.item().getAmount());
                }
            }

            paid.addAll(ofCurrency);
        }

        return paid;
    }

    private record Offer(ServerShopCategory category, ServerShopEntry entry) {
    }

    /** La línea de /tienda que compra cada material (la primera que lo compre, en el orden de /tienda). */
    private Map<Material, Offer> shopOffers(Player player) {

        Map<Material, Offer> offers = new LinkedHashMap<>();

        for (ServerShopCategory category : shopManager.ordered()) {

            if (!shopService.canEnter(player, category)) {
                continue;
            }

            for (ServerShopEntry entry : category.entries()) {

                if (entry.kind() != ServerShopEntry.Kind.MATERIAL || !entry.sellable()) {
                    continue;
                }

                Material material = Material.matchMaterial(entry.key().toUpperCase(Locale.ROOT));

                if (material != null) {
                    offers.putIfAbsent(material, new Offer(category, entry));
                }
            }
        }

        return offers;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

}
