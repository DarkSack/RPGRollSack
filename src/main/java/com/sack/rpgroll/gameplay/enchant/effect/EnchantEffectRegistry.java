package com.sack.rpgroll.gameplay.enchant.effect;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registro de tipos de efecto disponibles. Cada CustomEnchantment declara
 * un effectType (string) que debe coincidir con una clave registrada aquí.
 * Agregar un encantamiento nuevo que reutiliza un tipo existente no requiere
 * tocar este registro — solo YAML. Un tipo de efecto nuevo sí requiere
 * escribir un handler y registrarlo aquí.
 */
public class EnchantEffectRegistry {

    private final Map<String, EnchantEffectHandler> handlers = new HashMap<>();

    public void register(String effectType, EnchantEffectHandler handler) {
        handlers.put(effectType.toUpperCase(), handler);
    }

    public Optional<EnchantEffectHandler> get(String effectType) {
        if (effectType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(effectType.toUpperCase()));
    }

}