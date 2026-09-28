package com.sack.rpgroll.economy.api;

import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.wallet.EconomyResult;

import java.util.UUID;

/**
 * Guarda fuera de este servidor el saldo de algunas monedas: por ejemplo, una network que comparte
 * el dinero entre varios servidores. Otro plugin lo registra en el {@code ServicesManager} de Bukkit
 * ({@code register(WalletBackend.class, ...)}) y, desde ese momento, todo lo que mueve dinero en
 * RPGRoll-Economy (tiendas, subastas, Vault, /pay, comandos de admin) pasa por él para las monedas
 * que {@link #handles(String) diga}. Sin ninguno registrado, los saldos siguen en los ficheros del
 * servidor, como siempre.
 * <p>
 * Los métodos se llaman desde el hilo principal y deben responder rápido (una base de datos en la
 * misma máquina, con timeout corto). Si el almacén no responde, se devuelve
 * {@link EconomyResult#UNAVAILABLE} y no se mueve nada.
 */
public interface WalletBackend {

    /** Si este almacén lleva el saldo de esa moneda. */
    boolean handles(String currencyId);

    /** El saldo de un jugador ({@code NaN} si no se puede saber ahora: se trata como bloqueado). */
    double balance(UUID playerId, String currencyId);

    EconomyResult deposit(UUID playerId, String currencyId, double amount, TransactionType type, String description);

    EconomyResult withdraw(UUID playerId, String currencyId, double amount, TransactionType type,
            String description);

    /** De un jugador a otro de una vez: o se mueve todo o nada. */
    EconomyResult transfer(UUID fromId, UUID toId, String currencyId, double amount, String description);

    /**
     * Pasa al almacén el saldo que el jugador tenía en los ficheros de este servidor, la primera vez
     * que se usa esa moneda con el almacén activo. Tiene que ser idempotente por {@code importId}:
     * si el servidor cae después de pedirlo, se repite con el mismo id y no debe sumarse dos veces.
     */
    EconomyResult importBalance(UUID playerId, String currencyId, double amount, String importId);

}
