package com.sack.rpgroll.tab.context;

/** Solo aplica a condiciones de tipo {@link ContextConditionType#PLACEHOLDER}; el resto siempre compara por igualdad. */
public enum ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_EMPTY,
    EMPTY,
    GREATER_THAN,
    LESS_THAN,
    MATCHES
}
