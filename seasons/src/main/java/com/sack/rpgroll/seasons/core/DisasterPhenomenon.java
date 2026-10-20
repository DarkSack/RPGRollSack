package com.sack.rpgroll.seasons.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Un fenómeno de un {@link Disaster}: su tipo y los parámetros del YAML tal cual (texto, número,
 * booleano o lista). Cada tipo tiene un valor por defecto para todo; ver {@code disasters/_reference.yml}.
 */
public record DisasterPhenomenon(DisasterType type, Map<String, Object> params) {

    public DisasterPhenomenon {
        Objects.requireNonNull(type, "type no puede ser null");
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public double number(String key, double fallback) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean flag(String key, boolean fallback) {
        Object value = params.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value.toString().trim());
    }

    public String text(String key, String fallback) {
        Object value = params.get(key);
        return value == null ? fallback : value.toString().trim();
    }

    /** Una lista del YAML, o un texto con comas ({@code "DIRT, OAK_LEAVES"}); sin la clave, {@code fallback}. */
    public List<String> list(String key, List<String> fallback) {
        Object value = params.get(key);
        if (value == null) {
            return fallback;
        }
        List<String> items = value instanceof Collection<?> many
                ? many.stream().map(String::valueOf).map(String::trim).toList()
                : Arrays.stream(value.toString().split(",")).map(String::trim).toList();
        // Una lista vacía en el YAML es "ninguno", no "el de por defecto".
        return items.stream().filter(item -> !item.isEmpty()).toList();
    }

}
