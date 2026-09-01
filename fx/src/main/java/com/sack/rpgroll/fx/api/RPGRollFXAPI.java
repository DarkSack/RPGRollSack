package com.sack.rpgroll.fx.api;

import com.sack.rpgroll.fx.core.EffectManager;
import com.sack.rpgroll.fx.engine.EffectContext;
import com.sack.rpgroll.fx.engine.EffectEngine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Punto de entrada público de RPGRoll-FX para cualquier otro plugin
 * (de RPGRoll o no). Dos formas de usarla:
 * <ul>
 * <li>{@link #play(String, Player)} y sus variantes — dispara un efecto ya
 * definido por id (YAML o registrado por código), igual que un addon
 * consultaría RPGRollAPI.</li>
 * <li>{@link #builder()} — arma un efecto de una sola vez sin declarar
 * nada en YAML.</li>
 * </ul>
 */
public final class RPGRollFXAPI {

    private static RPGRollFXAPI instance;

    private final EffectManager effectManager;
    private final EffectEngine engine;

    private RPGRollFXAPI(EffectManager effectManager, EffectEngine engine) {
        this.effectManager = effectManager;
        this.engine = engine;
    }

    public static void init(EffectManager effectManager, EffectEngine engine) {
        instance = new RPGRollFXAPI(effectManager, engine);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /**
     * @throws IllegalStateException si RPGRoll-FX todavía no terminó de
     *                                arrancar. No debería ocurrir si tu plugin
     *                                declaró {@code depend/softdepend:
     *                                [RPGRoll-FX]}.
     */
    public static RPGRollFXAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-FX todavía no está listo.");
        }

        return instance;
    }

    // ============ Efectos definidos (YAML o registrados por código) ============

    /** @return true si el efecto existe y se disparó; false si no hay ningún efecto con ese id. */
    public boolean play(String effectId, Player caster) {
        return play(effectId, EffectContext.of(caster));
    }

    public boolean play(String effectId, Player caster, Entity target) {
        return play(effectId, EffectContext.of(caster, target));
    }

    public boolean play(String effectId, Player caster, Location targetLocation) {
        return play(effectId, EffectContext.of(caster, targetLocation));
    }

    private boolean play(String effectId, EffectContext context) {

        var effect = effectManager.get(effectId);

        if (effect.isEmpty()) {
            return false;
        }

        engine.play(effect.get(), context);
        return true;
    }

    public boolean exists(String effectId) {
        return effectManager.exists(effectId);
    }

    // ============ Constructor fluido (sin YAML) ============

    public EffectBuilder builder() {
        return new EffectBuilder(engine);
    }

    // ============ Acceso interno (GUI/comandos del propio addon) ============

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public EffectEngine getEngine() {
        return engine;
    }

}
