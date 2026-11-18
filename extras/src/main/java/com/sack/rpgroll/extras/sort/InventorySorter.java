package com.sack.rpgroll.extras.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Junta montones iguales hasta su tope y los ordena por grupo y luego por nombre. Genérico para
 * poder probarlo sin servidor; {@link ItemStackOps} lo aplica a ítems reales.
 */
public final class InventorySorter {

    /** Cómo leer y rehacer un montón. */
    public interface Ops<T> {
        boolean empty(T item);

        int group(T item);

        String name(T item);

        boolean similar(T a, T b);

        int amount(T item);

        int max(T item);

        T withAmount(T item, int amount);
    }

    private InventorySorter() {
    }

    public static <T> List<T> sort(List<T> items, Ops<T> ops) {
        List<T> out = new ArrayList<>();
        for (T item : items) {
            if (ops.empty(item)) {
                continue;
            }
            int left = ops.amount(item);
            for (int i = 0; i < out.size() && left > 0; i++) {
                T stack = out.get(i);
                int room = ops.max(stack) - ops.amount(stack);
                if (room > 0 && ops.similar(stack, item)) {
                    int moved = Math.min(room, left);
                    out.set(i, ops.withAmount(stack, ops.amount(stack) + moved));
                    left -= moved;
                }
            }
            int max = Math.max(1, ops.max(item));
            while (left > 0) {
                int part = Math.min(max, left);
                out.add(ops.withAmount(item, part));
                left -= part;
            }
        }
        // Orden estable: ítems iguales con datos distintos (nombres, encantamientos) quedan juntos.
        out.sort(Comparator.<T>comparingInt(ops::group).thenComparing(ops::name));
        return out;
    }
}
