package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.storage.MaterialRow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cuentas puras del libro de materiales: qué quitar al depositar, cuándo está completo, consumo y devolución. */
public final class DepositMath {

    private DepositMath() {
    }

    /** Cuánto quitar de cada material: lo que falta, como mucho lo disponible; {@code onlyKey} null = todos. */
    public static Map<String, Integer> toTake(List<MaterialSpec> specs, Map<String, MaterialRow> rows,
                                              Map<String, Integer> available, String onlyKey) {
        Map<String, Integer> take = new LinkedHashMap<>();
        for (MaterialSpec spec : specs) {
            String key = spec.key();
            MaterialRow row = rows.get(key);
            // Dos specs con la misma clave comparten fila: se calcula una sola vez.
            if (row == null || take.containsKey(key) || onlyKey != null && !onlyKey.equals(key)) {
                continue;
            }
            int n = Math.min(Math.max(0, row.required() - row.deposited()), available.getOrDefault(key, 0));
            if (n > 0) {
                take.put(key, n);
            }
        }
        return take;
    }

    /**
     * Todos los materiales no opcionales con lo exigido depositado. Un material sin fila en la obra (se añadió a la
     * estructura al recargar) cuenta como cumplido: no formaba parte de su precio.
     */
    public static boolean complete(List<MaterialSpec> specs, Map<String, MaterialRow> rows) {
        for (MaterialSpec spec : specs) {
            MaterialRow row = rows.get(spec.key());
            if (!spec.optional() && row != null && row.deposited() < row.required()) {
                return false;
            }
        }
        return true;
    }

    /** Claves de la estructura sin fila en la obra (sin repetir): lo que {@link #complete} da por cumplido. */
    public static List<String> missingRows(List<MaterialSpec> specs, Map<String, MaterialRow> rows) {
        return specs.stream().map(MaterialSpec::key).filter(k -> !rows.containsKey(k)).distinct().toList();
    }

    /** Consumo acumulado a {@code placed} de {@code total}: floor(required * placed / total), tope lo depositado. */
    public static Map<String, Integer> consumedAt(Map<String, MaterialRow> rows, int placed, int total) {
        Map<String, Integer> out = new LinkedHashMap<>();
        rows.forEach((key, row) -> {
            long n = total <= 0 ? 0 : (long) row.required() * placed / total;
            out.put(key, (int) Math.min(n, row.deposited()));
        });
        return out;
    }

    /** Lo no gastado por el porcentaje de devolución, redondeado hacia abajo. */
    public static Map<String, Integer> refund(Map<String, MaterialRow> rows, int percent) {
        Map<String, Integer> out = new LinkedHashMap<>();
        rows.forEach((key, row) -> out.put(key,
                (int) (Math.max(0, row.deposited() - row.consumed()) * (long) Math.max(0, Math.min(100, percent)) / 100)));
        return out;
    }

    /**
     * Dinero a devolver al cancelar: {@code round(cents × (total − cursor) / total × percent / 100)}; con
     * {@code total = 0}, todo por el porcentaje. Nunca más de lo cobrado ni menos de 0.
     */
    public static long moneyRefund(long cents, int total, int cursor, int percent) {
        double left = total <= 0 ? 1 : Math.max(0, Math.min(total, total - cursor)) / (double) total;
        return Math.max(0, Math.min(cents, Math.round(cents * left * Math.max(0, Math.min(100, percent)) / 100.0)));
    }
}
