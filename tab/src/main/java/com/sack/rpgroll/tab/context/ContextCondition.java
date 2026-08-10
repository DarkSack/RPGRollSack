package com.sack.rpgroll.tab.context;

/**
 * Una condición individual dentro de un {@link ContextDefinition}.
 * <p>
 * Para tipos "nativos" (WORLD/PERMISSION/GAMEMODE/DIMENSION/WEATHER):
 * {@code value} es el valor esperado, comparado por igualdad — {@code placeholder}
 * y {@code operator} se ignoran.
 * <p>
 * Para {@link ContextConditionType#PLACEHOLDER}: {@code placeholder} es la
 * plantilla a resolver (ej. {@code "{dungeon_id}"} o {@code "%rpgroll_level%"}),
 * {@code operator} determina cómo se compara, y {@code value} es el lado
 * derecho de la comparación (no usado por NOT_EMPTY/EMPTY).
 */
public record ContextCondition(
        ContextConditionType type,
        String placeholder,
        ConditionOperator operator,
        String value) {

    public static ContextCondition nativeCondition(ContextConditionType type, String value) {
        return new ContextCondition(type, null, ConditionOperator.EQUALS, value);
    }

    public static ContextCondition placeholderCondition(String placeholder, ConditionOperator operator, String value) {
        return new ContextCondition(ContextConditionType.PLACEHOLDER, placeholder, operator, value);
    }

}
