package com.sack.rpgroll.content;

/**
 * Contrato para cualquier servicio que deba recargarse cuando el
 * administrador ejecuta /rpg reload, sin que ReloadCommand necesite
 * conocer explícitamente cada tipo de contenido que existe.
 */
public interface Reloadable {
    void reload();
}