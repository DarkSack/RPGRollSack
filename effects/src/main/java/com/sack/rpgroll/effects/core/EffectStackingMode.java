package com.sack.rpgroll.effects.core;

/** Qué pasa cuando un efecto que ya está activo se vuelve a aplicar sobre el mismo objetivo. */
public enum EffectStackingMode {
    /** Reaplicar solo reinicia la duración — no hay stacks. */
    NONE,
    /** Cada aplicación agrega un stack independiente con su propia duración (todas corren en paralelo). */
    INDEPENDENT,
    /** Un único stack counter (tope {@code maxStacks}) que se incrementa y refresca la duración. */
    REFRESH,
    /** Igual que REFRESH, pero al llegar a {@code maxStacks} se reemplaza por {@code upgradeToEffectId}. */
    UPGRADE
}
