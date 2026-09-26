package com.sack.rpgroll.common.integration;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

/**
 * La economía que haya registrada en Vault, sin pasar por el core: cualquier
 * módulo cobra y paga igual esté o no RPGRoll instalado.
 * <p>
 * Se consulta en cada llamada (no se guarda) porque el proveedor puede
 * registrarse después de que el módulo arranque, o cambiar en una recarga.
 */
public final class VaultEconomy {

    private VaultEconomy() {
    }

    public static Optional<Economy> get() {

        // Sin Vault la clase Economy no existe: nunca tocarla antes de comprobarlo.
        if (Bukkit.getServer() == null || !Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return Optional.empty();
        }

        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        return provider == null ? Optional.empty() : Optional.ofNullable(provider.getProvider());
    }

    public static boolean isAvailable() {
        return get().isPresent();
    }

}
