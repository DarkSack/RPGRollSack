package com.sack.rpgroll.common.reskin;

/**
 * Configuración de un reskin visual: un ítem con {@code customModelData}
 * que se muestra en lugar (o encima) del modelo vanilla real de una
 * entidad viva, vía {@link EntityReskinService}. {@code material} null o
 * vacío = sin reskin (se usa el modelo vanilla normalmente).
 */
public record EntityReskin(String material, int customModelData, double scale, double yOffset) {

    public static final EntityReskin NONE = new EntityReskin(null, 0, 1.0, 0.0);

    public EntityReskin {
        material = material == null || material.isBlank() ? null : material.trim();
        scale = scale <= 0 ? 1.0 : scale;
    }

    public boolean isActive() {
        return material != null;
    }

}
