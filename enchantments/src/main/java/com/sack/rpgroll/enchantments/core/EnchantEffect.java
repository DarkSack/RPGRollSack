package com.sack.rpgroll.enchantments.core;

import java.util.Map;

/**
 * Un efecto concreto dentro de un encantamiento (ej. "HEAL amount: 2").
 * Los params son texto crudo tal como vienen del YAML — cada ejecutor de
 * efecto interpreta las claves que le interesan (amount, duration, etc.),
 * incluyendo placeholders "{clave}" resueltos contra el bloque de niveles.
 */
public record EnchantEffect(EnchantEffectType type, Map<String, String> params) {

    public EnchantEffect {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

}
