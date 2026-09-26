package com.sack.rpgroll.extras.backpack;

/**
 * Cuentas de pestañas: cada pestaña muestra hasta {@link #PAGE_SIZE}
 * espacios (5 filas) y la fila de abajo queda para los botones.
 */
public final class BackpackPages {

    public static final int PAGE_SIZE = 45;

    private BackpackPages() {
    }

    public static int pages(int slots) {
        return Math.max(1, (slots + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /**
     * Filas de contenido. Con varias pestañas siempre son 5, porque el tamaño
     * del inventario no cambia al pasar de una a otra.
     */
    public static int contentRows(int slots) {
        return pages(slots) > 1 ? 5 : Math.max(1, (slots + 8) / 9);
    }

    /** Tamaño del inventario: contenido más la fila de botones. */
    public static int inventorySize(int slots) {
        return (contentRows(slots) + 1) * 9;
    }

    /** Cuántos espacios usables tiene la pestaña {@code page} (desde 0). */
    public static int slotsOnPage(int slots, int page) {
        return Math.max(0, Math.min(PAGE_SIZE, slots - page * PAGE_SIZE));
    }

}
