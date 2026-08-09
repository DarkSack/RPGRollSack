package com.sack.rpgroll.economy.integration;

import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.inflation.InflationTracker;
import com.sack.rpgroll.economy.market.MarketEngine;
import com.sack.rpgroll.economy.tax.TaxRuleManager;
import com.sack.rpgroll.economy.tax.TaxType;
import com.sack.rpgroll.economy.wallet.WalletService;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Expansión de PlaceholderAPI de RPGRoll-Economy: {@code %rpgeconomy_<placeholder>%}.
 * <ul>
 * <li>{@code balance} / {@code balance_<moneda>} — saldo del jugador.</li>
 * <li>{@code bank} — suma de sus cuentas bancarias (moneda por defecto).</li>
 * <li>{@code currency} / {@code currency_symbol} — moneda por defecto.</li>
 * <li>{@code inflation} — % de inflación de la moneda por defecto.</li>
 * <li>{@code market_price_<producto>} — precio actual de mercado.</li>
 * <li>{@code tax_rate_<tipo>} — % total configurado para ese tipo de impuesto (sale/income/company/property/commercial/luxury).</li>
 * </ul>
 */
public class EconomyPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final WalletService walletService;
    private final CurrencyManager currencyManager;
    private final BankManager bankManager;
    private final MarketEngine marketEngine;
    private final TaxRuleManager taxRuleManager;
    private final InflationTracker inflationTracker;

    public EconomyPlaceholders(Plugin plugin, WalletService walletService, CurrencyManager currencyManager,
            BankManager bankManager, MarketEngine marketEngine, TaxRuleManager taxRuleManager,
            InflationTracker inflationTracker) {
        this.plugin = plugin;
        this.walletService = walletService;
        this.currencyManager = currencyManager;
        this.bankManager = bankManager;
        this.marketEngine = marketEngine;
        this.taxRuleManager = taxRuleManager;
        this.inflationTracker = inflationTracker;
    }

    @Override
    public String getIdentifier() {
        return "rpgeconomy";
    }

    @Override
    public String getAuthor() {
        return "Sack";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        String key = params.toLowerCase(Locale.ROOT);
        String defaultCurrencyId = currencyManager.defaultCurrency().id();

        if (key.equals("currency")) {
            return currencyManager.defaultCurrency().displayName();
        }

        if (key.equals("currency_symbol")) {
            return currencyManager.defaultCurrency().symbol();
        }

        if (key.equals("inflation")) {
            return formatNumber(inflationTracker.changePercent(defaultCurrencyId));
        }

        if (key.startsWith("market_price_")) {
            String productId = key.substring("market_price_".length());
            return productId.isBlank() ? "" : formatNumber(marketEngine.price(productId));
        }

        if (key.startsWith("tax_rate_")) {
            String typeName = key.substring("tax_rate_".length()).toUpperCase(Locale.ROOT);
            try {
                double total = taxRuleManager.rulesFor(TaxType.valueOf(typeName)).stream()
                        .mapToDouble(rule -> rule.ratePercent()).sum();
                return formatNumber(total);
            } catch (IllegalArgumentException e) {
                return "";
            }
        }

        if (player == null) {
            return "";
        }

        if (key.equals("balance")) {
            return formatNumber(walletService.balance(player.getUniqueId(), defaultCurrencyId));
        }

        if (key.startsWith("balance_")) {
            String currencyId = key.substring("balance_".length());
            return currencyId.isBlank() ? "" : formatNumber(walletService.balance(player.getUniqueId(), currencyId));
        }

        if (key.equals("bank")) {
            double total = 0;
            for (BankAccount account : bankManager.accountsOf(player.getUniqueId())) {
                total += account.balance(defaultCurrencyId);
            }
            return formatNumber(total);
        }

        return "";
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }

}
