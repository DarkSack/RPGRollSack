package com.sack.rpgroll.mobs.core;

import java.util.List;
import java.util.Map;

/**
 * Apariencia del mob: tipo base de entidad vanilla, escala, brillo,
 * invisibilidad, equipo visible y skins. {@code modelEngineId} es un
 * punto de extensión — si el server tiene ModelEngine/BetterModel
 * instalado, un addon puede leer este id y aplicar el modelo custom real;
 * este motor no incluye esa integración. {@code skins} es la lista de
 * skins propias del motor (sin depender de esos plugins externos): al
 * spawnear se sortea una por peso (vacía = sin reskin, mob vanilla normal),
 * aplicada vía {@link com.sack.rpgroll.common.reskin.EntityReskinService}.
 */
public record MobModel(
        String baseEntityType,
        double scale,
        boolean glow,
        boolean invisible,
        Map<String, String> equipment,
        String modelEngineId,
        List<MobSkin> skins) {

    public MobModel {
        equipment = equipment == null ? Map.of() : Map.copyOf(equipment);
        scale = scale <= 0 ? 1.0 : scale;
        skins = skins == null ? List.of() : List.copyOf(skins);
    }

    public static MobModel defaults(String baseEntityType) {
        return new MobModel(baseEntityType, 1.0, false, false, Map.of(), null, List.of());
    }

}
