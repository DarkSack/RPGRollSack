package com.sack.rpgroll.ascension.reward;

import java.util.List;
import java.util.Map;

/**
 * Lo que se entrega al desbloquear un logro, alcanzar un rango de facción o
 * evolucionar un oficio. Todo es opcional.
 *
 * @param money        dinero, por Vault
 * @param experience   experiencia de RPGRoll
 * @param talentPoints puntos de talento de Ascension
 * @param title        id de un título que se desbloquea
 * @param reputation   reputación por facción (puede ser negativa)
 * @param stats        bono permanente de atributo: {@code health} (vida) y
 *                     {@code speed} (velocidad, en %), los mismos que usan
 *                     evoluciones y talentos
 * @param items        ids de RPGRoll-Items que se entregan
 * @param commands     comandos de consola; {@code {player}} es el jugador
 * @param message      mensaje para el jugador
 * @param broadcast    anunciar el logro a todo el servidor
 */
public record Rewards(double money, int experience, int talentPoints, String title,
        Map<String, Integer> reputation, Map<String, Double> stats, List<String> items, List<String> commands,
        String message, boolean broadcast) {

    private static final Rewards NONE = new Rewards(0, 0, 0, null, Map.of(), Map.of(), List.of(), List.of(), null,
            false);

    public Rewards {
        reputation = reputation == null ? Map.of() : Map.copyOf(reputation);
        stats = stats == null ? Map.of() : Map.copyOf(stats);
        items = items == null ? List.of() : List.copyOf(items);
        commands = commands == null ? List.of() : List.copyOf(commands);
    }

    public static Rewards none() {
        return NONE;
    }

}
