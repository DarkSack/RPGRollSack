package com.sack.rpgroll.economy.integration;

import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

/**
 * Convierte a RPGRoll-Economy en PROVEEDOR del servicio Economy de Vault
 * (en vez de solo consumidor, como hace {@code core}) — opera siempre
 * sobre la moneda por defecto ({@code default-currency} en config.yml), así
 * que cualquier plugin/addon que hable Vault (Jobs, tiendas externas,
 * Guilds, Items, Workers) queda funcional automáticamente en cuanto este
 * addon está instalado. No implementa el soporte de "bancos" de Vault (es
 * un concepto distinto al banco propio de este addon) ni las variantes
 * por-mundo (una sola economía global).
 */
public class EconomyVaultProvider implements Economy {

    private final WalletService walletService;
    private final CurrencyManager currencyManager;

    public EconomyVaultProvider(WalletService walletService, CurrencyManager currencyManager) {
        this.walletService = walletService;
        this.currencyManager = currencyManager;
    }

    private Currency currency() {
        return currencyManager.defaultCurrency();
    }

    private UUID resolve(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return player.getUniqueId();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "RPGRoll-Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return currency().decimals();
    }

    @Override
    public String format(double amount) {
        return currency().format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return currency().displayName();
    }

    @Override
    public String currencyNameSingular() {
        return currency().displayName();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return walletService.balance(resolve(playerName), currency().id());
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return walletService.balance(player.getUniqueId(), currency().id());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return walletService.has(resolve(playerName), currency().id(), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return walletService.has(player.getUniqueId(), currency().id(), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdraw(resolve(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdraw(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return deposit(resolve(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return deposit(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    private EconomyResponse withdraw(UUID playerId, double amount) {

        EconomyResult result = walletService.withdraw(playerId, currency().id(), amount, TransactionType.MISC,
                "Vault: withdrawPlayer");

        double balance = walletService.balance(playerId, currency().id());

        return switch (result) {
            case SUCCESS -> new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, "");
            case INSUFFICIENT_FUNDS -> new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE,
                    "Fondos insuficientes");
            default -> new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, result.name());
        };
    }

    private EconomyResponse deposit(UUID playerId, double amount) {

        EconomyResult result = walletService.deposit(playerId, currency().id(), amount, TransactionType.MISC,
                "Vault: depositPlayer");

        double balance = walletService.balance(playerId, currency().id());

        return switch (result) {
            case SUCCESS -> new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, "");
            default -> new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, result.name());
        };
    }

    // ============ Bancos de Vault: no soportados (hasBankSupport() = false) ============

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    private EconomyResponse notImplemented() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "RPGRoll-Economy no soporta bancos de Vault (usa /economy bank en su lugar)");
    }

    // ============ Cuentas: RPGRoll-Economy no necesita crear cuentas explícitamente ============

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

}
