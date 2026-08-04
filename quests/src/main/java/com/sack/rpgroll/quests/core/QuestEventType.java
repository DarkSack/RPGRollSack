package com.sack.rpgroll.quests.core;

/**
 * Momento del ciclo de vida de una quest (o de un stage — comparten el
 * mismo vocabulario) en el que se pueden disparar acciones.
 */
public enum QuestEventType {
    ON_START,
    ON_PROGRESS,
    ON_COMPLETE,
    ON_FAIL,
    ON_ABANDON,
    ON_STAGE_CHANGE
}
