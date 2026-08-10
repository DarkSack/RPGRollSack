package com.sack.rpgroll.extras.modifier;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Map;

/**
 * Modificadores que una raza/clase/job de RPGRoll-Core aporta a los
 * sistemas de Extras (sección 18) — sin que Core sepa que Extras existe:
 * Extras lee el race/class/job ACTUAL del jugador vía la API pública de
 * RPGRoll y busca acá si hay un {@code ModifierSet} con ese mismo id.
 * <p>
 * Convención de claves en {@code values}: sufijo {@code _max} multiplica
 * el máximo de un stat, sufijo {@code _rate} multiplica sus tasas de
 * decay/regen, cualquier otra clave (ej. {@code cold_resistance}) es un
 * bono aditivo leído directamente por el sistema dueño de ese concepto
 * (ej. {@link com.sack.rpgroll.extras.thermal.ThermalProtectionService}).
 */
public record ModifierSet(String id, ModifierSourceType type, Map<String, Double> values) implements RPGContent {
}
