package com.sack.rpgroll.tab.tablist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Qué líneas de cabecera/pie le tocan a un jugador. */
public final class TablistLines {

    private TablistLines() {
    }

    /**
     * Con animación, el frame actual ocupa la primera línea (el título) y el resto se queda; antes sustituía todo
     * el bloque, y una cabecera animada no podía llevar más líneas. Sin frame, las líneas tal cual.
     */
    public static List<String> withAnimationFrame(List<String> lines, String frame) {
        if (frame == null) {
            return lines;
        }
        List<String> out = new ArrayList<>(lines);
        if (out.isEmpty()) {
            out.add(frame);
        } else {
            out.set(0, frame);
        }
        return out;
    }

    /**
     * Las líneas sin bloque o de un bloque que el jugador muestra, y cuya condición se cumple (o no tienen).
     *
     * @param blockShown     si el jugador muestra ese bloque
     * @param conditionTrue  si se cumple esa condición (ya con los placeholders del jugador)
     */
    public static List<String> visible(List<TablistLine> lines, Predicate<String> blockShown,
            Predicate<String> conditionTrue) {
        List<String> out = new ArrayList<>();
        for (TablistLine line : lines) {
            if (line.block() != null && !blockShown.test(line.block())) {
                continue;
            }
            if (line.condition() != null && !conditionTrue.test(line.condition())) {
                continue;
            }
            out.add(line.text());
        }
        return out;
    }
}
