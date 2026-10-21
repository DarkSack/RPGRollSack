package com.sack.rpgroll.disasters.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TsunamiTest {

    /** La ola de 12 bloques: el frente llega hasta la punta, la punta cuelga y debajo queda el tubo. */
    @Test
    void theWaveCurlsOverAHollowTube() {

        List<Tsunami.Slice> slices = Tsunami.profile(12, 0.5, 1.0);

        double reach = slices.stream().mapToDouble(Tsunami.Slice::forward).max().orElseThrow();
        assertEquals(6, reach, 1e-9);

        // El pie del frente es agua de abajo arriba.
        assertTrue(slices.stream().anyMatch(s -> s.forward() == 0 && s.bottom() == 0 && s.top() == 11));

        // La punta cuelga hasta la mitad, mucho más abajo de donde llega el frente a esa distancia.
        Tsunami.Slice tip = slices.stream().filter(s -> s.tip() && s.forward() == reach).findFirst().orElseThrow();
        assertEquals(6, tip.bottom());

        // A media distancia el agua empieza arriba: debajo está el hueco del tubo.
        Tsunami.Slice middle = slices.stream().filter(s -> !s.tip() && s.forward() > 2.5 && s.forward() < 3.5
                && s.top() == 11).findFirst().orElseThrow();
        assertTrue(middle.bottom() >= 6, "hueco bajo la cresta: empieza en " + middle.bottom());

        // Detrás, el lomo baja hasta el suelo y cada vez es más bajo.
        List<Tsunami.Slice> back = slices.stream().filter(s -> s.forward() < 0).toList();
        assertTrue(back.size() > 10);
        for (int i = 1; i < back.size(); i++) {
            assertTrue(back.get(i).top() <= back.get(i - 1).top());
        }
    }

    /** En las puntas de la ola (columnas bajas) no hay rizo: un poco de agua y ya. */
    @Test
    void shortColumnsDoNotCurl() {
        List<Tsunami.Slice> slices = Tsunami.profile(1, 0.5, 1.0);
        assertTrue(slices.stream().allMatch(s -> s.forward() <= 1));
        assertTrue(slices.stream().anyMatch(s -> s.forward() == 0 && s.bottom() == 0 && s.top() == 0));
    }
}
