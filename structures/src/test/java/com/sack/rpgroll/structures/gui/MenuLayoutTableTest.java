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
        assertEquals(List.of("centro", "mejoras", "acceso", "ayuda", "confirmar", "categorias", "lista", "ficha",
                        "materiales", "vista", "obra", "construcciones"),
                StructuresMenus.LAYOUTS.stream().map(Layout::id).toList());
    }

    /** Columnas 1 a 7 de {@code rows} filas desde {@code firstRow}. */
    static List<Integer> grid(int firstRow, int rows) {
        return IntStream.range(firstRow, firstRow + rows).boxed()
                .flatMap(r -> range(r * 9 + 1, r * 9 + 7).stream()).toList();
    }

    static List<Integer> concat(List<Integer> a, Integer... b) {
        return Stream.concat(a.stream(), Stream.of(b)).toList();
    }

    @Test void newMenuTablesMatchTheBrief() {
        assertEquals(new Layout("categorias", 6, List.of(), concat(grid(1, 3), 49)), StructuresMenus.CATEGORIAS);
        assertEquals(new Layout("lista", 6, List.of(49), Stream.concat(Stream.of(4), concat(grid(1, 4), 45, 53)
                .stream()).toList()), StructuresMenus.LISTA);
        assertEquals(new Layout("ficha", 6, List.of(49), Stream.concat(Stream.of(4), concat(grid(2, 2), 47, 51)
                .stream()).toList()), StructuresMenus.FICHA);
        assertEquals(new Layout("materiales", 6, List.of(45, 47, 49),
                Stream.concat(Stream.of(4), concat(grid(1, 4), 51, 53).stream()).toList()), StructuresMenus.MATERIALES);
        assertEquals(new Layout("vista", 3, concat(range(10, 16), 18, 26), List.of(4, 22)), StructuresMenus.VISTA);
        assertEquals(new Layout("obra", 4, List.of(22, 24, 31), List.of(4, 13, 20)), StructuresMenus.OBRA);
        assertEquals(new Layout("construcciones", 6, List.of(), concat(range(0, 44), 45, 49, 53)),
                StructuresMenus.CONSTRUCCIONES);
        assertEquals(grid(1, 3), StructuresMenus.grid(1, 3));
    }

    @Test void slotsAreUniqueInEveryTable() {
        for (Layout l : StructuresMenus.LAYOUTS) {
            List<Integer> all = Stream.concat(l.painted().stream(), l.visible().stream()).toList();
            assertEquals(all.size(), new HashSet<>(all).size(), l.id() + " repite casillas");
        }
    }

    /** Las casillas de los menús nuevos están en su tabla, en el lado correcto (true = dibujada). */
    @Test void newMenuSlotsSitOnTheRightSideOfTheTable() {
        Map<Layout, Map<Integer, Boolean>> used = Map.of(
                StructuresMenus.CATEGORIAS, Map.of(CategoriesMenu.VOLVER, false,
                        CategoriesMenu.CONTENT.getFirst(), false, CategoriesMenu.CONTENT.getLast(), false),
                StructuresMenus.LISTA, Map.of(ListMenu.CATEGORIA, false, ListMenu.ANTERIOR, false,
                        ListMenu.VOLVER, true, ListMenu.SIGUIENTE, false, ListMenu.CONTENT.getFirst(), false,
                        ListMenu.CONTENT.getLast(), false),
                StructuresMenus.FICHA, Map.of(DetailMenu.ICONO, false, DetailMenu.VISTA_PREVIA, false,
                        DetailMenu.VOLVER, true, DetailMenu.ELEGIR, false, DetailMenu.MATERIALES.getFirst(), false,
                        DetailMenu.MATERIALES.getLast(), false),
                StructuresMenus.MATERIALES, Map.of(MaterialsMenu.INFO, false, MaterialsMenu.DEPOSITAR, true,
                        MaterialsMenu.RECUPERAR, true, MaterialsMenu.VOLVER, true, MaterialsMenu.CONSTRUIR, false,
                        MaterialsMenu.RECOGER, false, MaterialsMenu.CONTENT.getFirst(), false,
                        MaterialsMenu.CONTENT.getLast(), false),
                StructuresMenus.VISTA, Map.of(PreviewMenu.INFO, false, PreviewMenu.MODO, false,
                        PreviewMenu.X_MENOS, true, PreviewMenu.Z_MAS, true, PreviewMenu.GIRAR, true,
                        PreviewMenu.CANCELAR, true, PreviewMenu.CONFIRMAR, true, PreviewMenu.Y_MAS, true),
                StructuresMenus.OBRA, Map.of(ProjectMenu.INFO, false, ProjectMenu.PROGRESO, false,
                        ProjectMenu.PAUSA, false, ProjectMenu.MATERIALES, true, ProjectMenu.CANCELAR, true,
                        ProjectMenu.VOLVER, true),
                StructuresMenus.CONSTRUCCIONES, Map.of(BuildsMenu.ANTERIOR, false, BuildsMenu.VOLVER, false,
                        BuildsMenu.SIGUIENTE, false, 0, false, BuildsMenu.PAGE_SIZE - 1, false));
        used.forEach((layout, slots) -> slots.forEach((slot, painted) -> {
            assertEquals(painted, layout.isPainted(slot), layout.id() + " " + slot);
            if (!painted) {
                assertTrue(layout.visible().contains(slot), layout.id() + " " + slot + " no está en visible");
            }
        }));
        assertEquals(ListMenu.PAGE_SIZE, ListMenu.CONTENT.size());
        assertEquals(28, ListMenu.PAGE_SIZE);
        assertEquals(7, List.of(PreviewMenu.X_MENOS, PreviewMenu.X_MAS, PreviewMenu.Y_MENOS, PreviewMenu.Y_MAS,
                PreviewMenu.Z_MENOS, PreviewMenu.Z_MAS, PreviewMenu.GIRAR).stream().distinct().count());
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
