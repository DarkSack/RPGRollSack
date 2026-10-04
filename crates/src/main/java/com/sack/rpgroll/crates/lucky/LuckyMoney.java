package com.sack.rpgroll.crates.lucky;

import com.sack.rpgroll.common.integration.VaultEconomy;

import org.bukkit.entity.Player;

/**
 * El pago por Vault, aparte: si un listener nombra la clase Economy y Vault no
 * está instalado, Bukkit no puede registrar ninguno de sus eventos.
 */
final class LuckyMoney {

    private LuckyMoney() {
    }

    /** Ingresa {@code amount}; false si no hay economía. */
    static boolean deposit(Player player, double amount) {
        if (!VaultEconomy.isAvailable()) {
            return false;
        }
        VaultEconomy.get().ifPresent(economy -> economy.depositPlayer(player, amount));
        return true;
    }

}
