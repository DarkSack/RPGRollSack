package com.sack.rpgroll.common.price;

import org.bukkit.entity.Player;

/**
 * Algo del jugador que le mueve los precios cuando compra o vende al
 * servidor: su carisma (el core), su reputación con una facción (Ascension).
 * Se registra con {@link Prices#register}.
 */
public interface PriceSource {

    /**
     * @param faction la facción dueña de la tienda, o {@code null} si no es de ninguna
     * @return fracción a favor del jugador (0.10 = un 10 % mejor para él); negativa, en contra
     */
    double discount(Player player, String faction);

}
