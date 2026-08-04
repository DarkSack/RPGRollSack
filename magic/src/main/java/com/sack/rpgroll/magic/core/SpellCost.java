package com.sack.rpgroll.magic.core;

/**
 * Requisitos para lanzar un {@link Spell}. Almas/energía y otras monedas
 * custom quedan fuera de esta primera pasada — no hay todavía ningún addon
 * de economía propio que las respalde; mana/vida/experiencia reusan el
 * estado real del jugador (CombatStats/RPGPlayer de :core) y el reactivo
 * ({@code reagentMaterial}) descuenta un ítem real del inventario.
 *
 * @param mana            costo de maná (0 = sin costo de maná)
 * @param health          costo de vida (0 = sin costo de vida)
 * @param experience      costo de experiencia de RPGRoll (0 = sin costo)
 * @param reagentMaterial material del reactivo a consumir del inventario, o null si no requiere ninguno
 * @param reagentAmount   cantidad de ese material a consumir
 */
public record SpellCost(int mana, int health, int experience, String reagentMaterial, int reagentAmount) {

    public SpellCost {
        mana = Math.max(0, mana);
        health = Math.max(0, health);
        experience = Math.max(0, experience);
        reagentMaterial = (reagentMaterial == null || reagentMaterial.isBlank()) ? null : reagentMaterial;
        reagentAmount = Math.max(1, reagentAmount);
    }

    public static SpellCost none() {
        return new SpellCost(0, 0, 0, null, 1);
    }

    public boolean hasReagent() {
        return reagentMaterial != null;
    }

}
