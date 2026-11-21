package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.common.integration.VaultEconomy;

import org.bukkit.entity.Player;

/**
 * El dinero del Desencantador, por Vault. Vive aparte para que ninguna clase que se registre como
 * listener nombre {@code Economy}: sin Vault, esa clase no existe y el registro fallaría.
 */
final class DisenchantMoney {

    private DisenchantMoney() {
    }

    static boolean available() {
        return VaultEconomy.isAvailable();
    }

    static boolean has(Player player, double amount) {
        return VaultEconomy.get().map(economy -> economy.has(player, amount)).orElse(false);
    }

    static boolean take(Player player, double amount) {
        return VaultEconomy.get().map(economy -> economy.withdrawPlayer(player, amount).transactionSuccess())
                .orElse(false);
    }

    static String format(double amount) {
        return VaultEconomy.get().map(economy -> economy.format(amount))
                .orElseGet(() -> String.valueOf((long) amount));
    }
}
