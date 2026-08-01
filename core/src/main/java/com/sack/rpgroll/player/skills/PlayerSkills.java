package com.sack.rpgroll.player.skills;

import java.util.*;

/**
 * Record que representa las habilidades aprendidas por un jugador.
 */
public record PlayerSkills(Map<String, Integer> learnedSkills) {

    /**
     * Crea PlayerSkills vacío (sin habilidades).
     */
    public static PlayerSkills empty() {
        return new PlayerSkills(new HashMap<>());
    }

    /**
     * Obtiene el nivel de una habilidad aprendida, o 0 si no la ha aprendido.
     */
    public int getSkillLevel(String skillId) {
        return learnedSkills.getOrDefault(skillId, 0);
    }

    /**
     * Verifica si el jugador ha aprendido una habilidad.
     */
    public boolean hasSkill(String skillId) {
        return learnedSkills.containsKey(skillId) && learnedSkills.get(skillId) > 0;
    }

    /**
     * Aprende una nueva habilidad (nivel 1).
     */
    public PlayerSkills learn(String skillId) {
        Map<String, Integer> updated = new HashMap<>(learnedSkills);
        updated.put(skillId, 1);
        return new PlayerSkills(updated);
    }

    /**
     * Sube el nivel de una habilidad.
     */
    public PlayerSkills upgradeSkill(String skillId) {
        Map<String, Integer> updated = new HashMap<>(learnedSkills);
        updated.put(skillId, getSkillLevel(skillId) + 1);
        return new PlayerSkills(updated);
    }

    /**
     * Obtiene todas las habilidades aprendidas.
     */
    public Set<String> getLearnedSkillIds() {
        return learnedSkills.keySet();
    }

}
