package com.sack.rpgroll.quests.core;

import java.util.Map;

/**
 * Un objetivo dentro de un stage (ej. "KILL_ENTITY entity=ZOMBIE amount=20").
 * El tipo es texto libre — el motor trae tipos base listos para usar
 * (ver {@link com.sack.rpgroll.quests.registry.QuestObjectiveTypes}) y los
 * addons pueden sumar los suyos llamando a su progreso directamente vía
 * {@code QuestEngine#progressObjective(...)}, sin registrar una clase nueva.
 */
public record QuestObjective(String type, String description, int amount, Map<String, String> params) {

    public QuestObjective {
        description = description == null ? "" : description;
        amount = Math.max(amount, 1);
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

    /**
     * @return true si esta definición de objetivo "matchea" los parámetros
     *         de un evento de progreso concreto. Solo se comparan las claves
     *         que el objetivo declaró explícitamente — cualquier clave que
     *         el objetivo no mencione se trata como comodín (aplica a todos).
     */
    public boolean matches(Map<String, String> eventParams) {

        for (var entry : params.entrySet()) {

            if (entry.getKey().equals("amount")) {
                continue;
            }

            String eventValue = eventParams.get(entry.getKey());

            if (eventValue == null || !eventValue.equalsIgnoreCase(entry.getValue())) {
                return false;
            }
        }

        return true;
    }

}
