package com.sack.rpgroll.workers.core.ai;

/** Qué hace un worker cuando una {@link AiCondition} matchea. */
public enum AiAction {
    SEEK_FOOD,
    EAT,
    GO_TO_WAREHOUSE,
    STORE_ITEMS,
    SEEK_SHELTER,
    GO_HOME,
    SLEEP,
    WORK,
    IDLE
}
