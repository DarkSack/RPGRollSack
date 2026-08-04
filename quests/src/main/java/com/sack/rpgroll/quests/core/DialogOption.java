package com.sack.rpgroll.quests.core;

/**
 * Una rama de decisión al final de un diálogo. {@code nextStage} es el id
 * del stage al que salta la quest si el jugador elige esta opción — permite
 * armar árboles de decisión (aceptar / rechazar / pedir info, etc.).
 */
public record DialogOption(String label, String nextStage) {
}
