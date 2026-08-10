package com.sack.rpgroll.tab.placeholder;

import org.bukkit.entity.Player;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Punto de registro público de placeholders para RPGRoll-TAB. Cualquier
 * plugin (incluyendo los propios addons de RPGRoll) puede registrar un
 * placeholder {@code {clave}} sin que TAB tenga que conocer nada de su
 * dominio — es la pieza clave de "el plugin no tiene lógica de RPG
 * hardcodeada".
 */
public interface TABPlaceholderRegistry {

    /** Registra un placeholder {@code {key}} que depende del jugador que lo ve. */
    void register(String key, Function<Player, String> resolver);

    /** Registra un placeholder {@code {key}} global (no depende de un jugador, ej. {online}/{max}). */
    void registerGlobal(String key, Supplier<String> resolver);

    /** Quita un placeholder previamente registrado (por clave, sin importar si era player-scoped o global). */
    void unregister(String key);

}
