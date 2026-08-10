package com.sack.rpgroll.tab.belowname;

import com.sack.rpgroll.common.content.RPGContent;

/**
 * Vanilla solo permite un NÚMERO debajo del nombre (limitación del cliente,
 * no de RPGRoll-TAB) — {@code scorePlaceholder} debe resolver a algo
 * numérico (ej. {@code "{health}"}), {@code label} es el texto/ícono que
 * aparece junto al número (nombre del objective, ej. {@code "&c❤"}).
 */
public record BelowNameDefinition(String id, String scorePlaceholder, String label) implements RPGContent {
}
