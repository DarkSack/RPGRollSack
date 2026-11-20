package com.sack.rpgroll.emotes.service;

/** Resultado de intentar lanzar un emote. */
public enum LaunchResult {
    OK, UNKNOWN, LOCKED, COOLDOWN, BUSY_STATE, IN_COMBAT, ENGINE_FAILED, EMPTY_FAVORITE
}
