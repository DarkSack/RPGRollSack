package com.sack.rpgroll.mobs.core;

import java.util.Map;

/**
 * Apariencia del mob: tipo base de entidad vanilla, escala, brillo,
 * invisibilidad y equipo visible. {@code modelEngineId} es un punto de
 * extensión — si el server tiene ModelEngine/BetterModel instalado, un
 * addon puede leer este id y aplicar el modelo custom real; este motor no
 * incluye esa integración (requeriría esas dependencias reales).
 */
public record MobModel(
        String baseEntityType,
        double scale,
        boolean glow,
        boolean invisible,
        Map<String, String> equipment,
        String modelEngineId) {

    public MobModel {
        equipment = equipment == null ? Map.of() : Map.copyOf(equipment);
        scale = scale <= 0 ? 1.0 : scale;
    }

    public static MobModel defaults(String baseEntityType) {
        return new MobModel(baseEntityType, 1.0, false, false, Map.of(), null);
    }

}
