package com.sack.rpgroll.pass.mission;

/**
 * @param target filtro dentro del tipo en mayúsculas o id; cadena vacía = cualquiera
 * @param xp     puntos de pase al completarla
 */
public record Mission(String id, String name, MissionScope scope, MissionType type, String target, int amount,
        int xp) {

    public boolean matches(MissionType type, String target) {
        return this.type == type && (this.target.isEmpty() || this.target.equalsIgnoreCase(target));
    }

}
