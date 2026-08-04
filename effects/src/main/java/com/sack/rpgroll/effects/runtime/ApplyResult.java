package com.sack.rpgroll.effects.runtime;

/** Qué pasó al intentar aplicar un efecto — el llamador decide qué hacer con cada caso. */
public enum ApplyResult {
    /** No había una instancia previa — se creó una nueva. */
    APPLIED,
    /** Ya estaba activo (modo NONE) — se reinició la duración. */
    REFRESHED,
    /** Ya estaba activo (modo REFRESH/INDEPENDENT) — se sumó un stack. */
    STACKED,
    /** Llegó al tope de stacks en modo UPGRADE — el llamador debe aplicar {@code upgradeToEffectId} y remover este. */
    UPGRADE_READY,
    /** El objetivo es inmune a alguno de los tags del efecto — no se aplicó nada. */
    BLOCKED_IMMUNE
}
