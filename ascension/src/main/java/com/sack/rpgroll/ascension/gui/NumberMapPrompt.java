package com.sack.rpgroll.ascension.gui;

import java.util.HashMap;
import java.util.Map;

/** Formato de línea de chat para mapas String-&gt;Double: clave1=valor1,clave2=valor2 */
public final class NumberMapPrompt {

    private NumberMapPrompt() {
    }

    public static String format(Map<String, Double> map) {

        StringBuilder builder = new StringBuilder();

        map.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(key).append('=').append(value);
        });

        return builder.toString();
    }

    public static Map<String, Double> parse(String line) {

        Map<String, Double> result = new HashMap<>();

        if (line == null || line.isBlank()) {
            return result;
        }

        for (String entry : line.split(",")) {
            String[] kv = entry.split("=");
            if (kv.length == 2) {
                result.put(kv[0].trim().toLowerCase(), Double.parseDouble(kv[1].trim()));
            }
        }

        return result;
    }

}
