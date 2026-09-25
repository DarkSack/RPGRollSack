package com.sack.rpgroll.ascension.progress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lee listas de criterios ({@code criteria:} de un logro, {@code sources:} de una facción). */
public final class CriterionParser {

    private CriterionParser() {
    }

    /**
     * @param onlyCounters las fuentes de reputación solo admiten criterios
     *                     que llegan como evento: un criterio de estado no
     *                     "pasa", así que no tendría cuándo dar reputación
     */
    public static List<Criterion> parseList(String ownerId, List<Map<?, ?>> entries, boolean onlyCounters) {

        List<Criterion> criteria = new ArrayList<>();

        for (Map<?, ?> entry : entries) {

            Object rawType = entry.get("type");
            if (rawType == null) {
                throw new IllegalArgumentException("'" + ownerId + "': criterio sin 'type'");
            }

            TriggerType type;
            try {
                type = TriggerType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("'" + ownerId + "': tipo de criterio desconocido '" + rawType + "'");
            }

            if (onlyCounters && !type.isCounter()) {
                throw new IllegalArgumentException("'" + ownerId + "': " + type
                        + " no puede ser fuente de reputación (no es un evento)");
            }

            criteria.add(new Criterion(type, string(entry.get("target")), string(entry.get("held")),
                    string(entry.get("key")), integer(entry.get("amount"), 1)));
        }

        return criteria;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static int integer(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
                // cae al valor por defecto
            }
        }
        return fallback;
    }

}
