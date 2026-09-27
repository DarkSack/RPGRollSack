package com.sack.rpgroll.furniture.core;

import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;

/**
 * Cómo se dibuja el modelo.
 *
 * @param scale        escala del modelo (1 = tamaño del modelo tal cual)
 * @param translation  desplazamiento del modelo desde el centro de su bloque, en bloques y en
 *                     el marco del mueble mirando al sur
 * @param brightness   luz fija 0-15 con la que se dibuja, o -1 para la del sitio
 * @param viewRange    distancia de visión (1 = la normal de una entidad, unos 64 bloques)
 * @param shadowRadius sombra bajo el mueble (0 = sin sombra)
 * @param transform    contexto de dibujo del modelo (FIXED, como en un marco)
 */
public record DisplaySettings(float scale, Offset translation, int brightness, float viewRange,
        float shadowRadius, ItemDisplayTransform transform) {

    public static final DisplaySettings DEFAULT =
            new DisplaySettings(1f, Offset.ZERO, -1, 1f, 0f, ItemDisplayTransform.FIXED);
}
