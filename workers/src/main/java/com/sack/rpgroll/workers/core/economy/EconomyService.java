package com.sack.rpgroll.workers.core.economy;

import com.sack.rpgroll.api.RPGRollAPI;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Cobra el salario de un worker de la cuenta Vault real de su empleador
 * — reusa {@code RPGRollAPI.get().getEconomyProvider()} (ya cableado en
 * :core con EssentialsX/CMI/etc.) en vez de inventar una economía
 * propia. El worker en sí no tiene cuenta — "cobrar" es, en la práctica,
 * un costo de mantenimiento que el empleador paga para seguir teniendo
 * el contrato activo.
 */
public class EconomyService {

    /** @return true si se pudo cobrar (fondos suficientes y economía disponible). */
    public boolean charge(UUID employerId, double amount) {

        if (amount <= 0) {
            return true;
        }

        if (!RPGRollAPI.isReady()) {
            return false;
        }

        var economyProvider = RPGRollAPI.get().getEconomyProvider();

        if (economyProvider == null || !economyProvider.isAvailable()) {
            return false;
        }

        return economyProvider.getEconomy().map(economy -> {

            OfflinePlayer employer = Bukkit.getOfflinePlayer(employerId);

            if (!economy.has(employer, amount)) {
                return false;
            }

            economy.withdrawPlayer(employer, amount);
            return true;

        }).orElse(false);
    }

}
