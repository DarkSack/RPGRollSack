package com.sack.rpgroll.tab.options;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lo que un jugador eligió mostrar ({@code 1}) u ocultar ({@code 0}) en su cabecera y pie, por bloque.
 * Un bloque sin elección usa el valor por defecto del tablist. Se guarda como {@code "personaje=0,servidor=1"}.
 */
public final class BlockChoices {

    private final Map<String, Boolean> shown;

    private BlockChoices(Map<String, Boolean> shown) {
        this.shown = shown;
    }

    public static BlockChoices parse(String raw) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                int eq = part.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String id = part.substring(0, eq).trim();
                String value = part.substring(eq + 1).trim();
                if (!id.isEmpty() && (value.equals("1") || value.equals("0"))) {
                    map.put(id, value.equals("1"));
                }
            }
        }
        return new BlockChoices(map);
    }

    public boolean isShown(String block, boolean byDefault) {
        return shown.getOrDefault(block, byDefault);
    }

    /** Una copia con ese bloque al revés de como se ve ahora. */
    public BlockChoices toggle(String block, boolean byDefault) {
        Map<String, Boolean> copy = new LinkedHashMap<>(shown);
        copy.put(block, !isShown(block, byDefault));
        return new BlockChoices(copy);
    }

    public String serialize() {
        return shown.entrySet().stream()
                .map(e -> e.getKey() + "=" + (e.getValue() ? "1" : "0"))
                .collect(Collectors.joining(","));
    }
}
