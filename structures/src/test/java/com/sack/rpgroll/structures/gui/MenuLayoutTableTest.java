package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.structures.gui.StructuresMenus.Layout;
import com.sack.rpgroll.structures.workbench.WorkbenchService.AccessChange;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** La tabla de casillas dibujadas que el generador de fondos copia. Si cambia aquí, cambian los fondos. */
class MenuLayoutTableTest {

    static List<Integer> range(int from, int toInclusive) {
        return IntStream.rangeClosed(from, toInclusive).boxed().toList();
    }

    @Test void tableMatchesTheBackgroundBrief() {
        assertEquals(new Layout("centro", 6, List.of(49, 53), List.of(4, 20, 22, 24, 29, 31, 33)),
                StructuresMenus.CENTRO);
        assertEquals(new Layout("mejoras", 4, List.of(31),
                Stream.concat(Stream.of(4, 22), range(10, 16).stream()).toList()), StructuresMenus.MEJORAS);
        assertEquals(new Layout("acceso", 6, List.of(48),
                Stream.concat(range(0, 44).stream(), Stream.of(45, 50, 53)).toList()), StructuresMenus.ACCESO);
        assertEquals(new Layout("ayuda", 3, List.of(), List.of(13, 18, 22, 26)), StructuresMenus.AYUDA);
        assertEquals(new Layout("confirmar", 3, List.of(11, 15), List.of(13)), StructuresMenus.CONFIRMAR);
        assertEquals(List.of("centro", "mejoras", "acceso", "ayuda", "confirmar"),
                StructuresMenus.LAYOUTS.stream().map(Layout::id).toList());
    }

    @Test void paintedAndVisibleNeverOverlapAndFitTheMenu() {
        for (Layout l : StructuresMenus.LAYOUTS) {
            Set<Integer> painted = new HashSet<>(l.painted());
            for (int s : l.visible()) {
                assertFalse(painted.contains(s), l.id() + ": " + s);
            }
            Stream.concat(l.painted().stream(), l.visible().stream())
                    .forEach(s -> assertTrue(s >= 0 && s < l.rows() * 9, l.id() + ": " + s));
        }
    }

    /** Cada casilla que usa el código está en la tabla, en el lado correcto. */
    @Test void menuSlotsSitOnTheRightSideOfTheTable() {
        Map<Layout, Map<Integer, Boolean>> used = Map.of(
                StructuresMenus.CENTRO, Map.of(CenterMenu.AYUDA, true, CenterMenu.CERRAR, true,
                        CenterMenu.NIVEL, false, CenterMenu.CATALOGO, false, CenterMenu.MIS_CONSTRUCCIONES, false,
                        CenterMenu.MATERIALES, false, CenterMenu.OBRA, false, CenterMenu.MEJORAS, false,
                        CenterMenu.ACCESO, false),
                StructuresMenus.MEJORAS, Map.of(UpgradeMenu.VOLVER, true, UpgradeMenu.NIVEL, false,
                        UpgradeMenu.MEJORAR, false, UpgradeMenu.REQUISITOS_DESDE, false,
                        UpgradeMenu.REQUISITOS_DESDE + UpgradeMenu.REQUISITOS_MAX - 1, false),
                StructuresMenus.ACCESO, Map.of(AccessMenu.VOLVER, true, AccessMenu.ANTERIOR, false,
                        AccessMenu.ANADIR, false, AccessMenu.SIGUIENTE, false, 0, false, AccessMenu.PAGE_SIZE - 1, false),
                StructuresMenus.AYUDA, Map.of(HelpMenu.TEXTO, false, HelpMenu.ANTERIOR, false, HelpMenu.VOLVER, false,
                        HelpMenu.SIGUIENTE, false),
                StructuresMenus.CONFIRMAR, Map.of(ConfirmMenu.CONFIRM, true, ConfirmMenu.CANCEL, true,
                        ConfirmMenu.INFO, false));
        used.forEach((layout, slots) -> slots.forEach((slot, painted) -> {
            assertEquals(painted, layout.isPainted(slot), layout.id() + " " + slot);
            if (!painted) {
                assertTrue(layout.visible().contains(slot), layout.id() + " " + slot + " no está en visible");
            }
        }));
    }

    @Test void menuTextIsNeverItalic() {
        Component c = StructuresMenus.plain(Component.text("Mejoras").append(Component.text(" x")));
        assertEquals(TextDecoration.State.FALSE, c.decoration(TextDecoration.ITALIC));
        StructuresMenus.plain(List.of(Component.text("a"), Component.text("b")))
                .forEach(l -> assertEquals(TextDecoration.State.FALSE, l.decoration(TextDecoration.ITALIC)));
    }

    @Test void trustedListIsSortedByNameNotUuid() {
        UUID zed = new UUID(0, 1);
        UUID ana = new UUID(0, 2);
        UUID unknown = new UUID(0, 3);
        Map<UUID, String> names = new java.util.HashMap<>(Map.of(zed, "Zed", ana, "ana"));
        assertEquals(List.of(unknown, ana, zed), AccessMenu.sortedByName(Set.of(zed, ana, unknown), names::get));
    }

    @Test void accessMessagesSayWhatHappened() {
        assertEquals("access.added", AccessMenu.messageKey(AccessChange.DONE, "access.added"));
        assertEquals("access.already", AccessMenu.messageKey(AccessChange.UNCHANGED, "access.added"));
        assertEquals("access.error", AccessMenu.messageKey(AccessChange.DENIED, "access.added"));
        assertEquals("access.error", AccessMenu.messageKey(null, "access.added"));
    }
}
