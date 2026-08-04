package com.sack.rpgroll.items.gui;

import com.sack.rpgroll.gui.InventoryGUI;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Base para GUIs paginadas: reserva la última fila para navegación (anterior
 * en {@code navSlot(0)}, siguiente en {@code navSlot(8)}) y expone un grid
 * de contenido de {@code contentSlotsPerPage} slots en las filas restantes.
 * Las subclases solo implementan qué hay en cada slot de contenido y qué
 * pasa al clickearlo — la paginación, el grid-to-index mapping y los
 * botones de navegación son comunes a todas.
 */
public abstract class PaginatedGUI extends InventoryGUI {

    protected final int contentSlotsPerPage;
    protected int page = 0;

    private final Map<Integer, Integer> slotToIndex = new HashMap<>();

    protected PaginatedGUI(Player player, Component title, int size, int contentSlotsPerPage) {
        super(player, title, size);
        this.contentSlotsPerPage = contentSlotsPerPage;
    }

    /** Cantidad total de elementos navegables (no de slots — de ítems lógicos). */
    protected abstract int totalItemCount();

    /** Renderiza el elemento {@code absoluteIndex} en el slot de contenido {@code contentSlot}. */
    protected abstract void renderItem(int contentSlot, int absoluteIndex);

    /** Botones fijos (filtros, atrás, crear, etc.) fuera del grid de contenido — llamado después del grid. */
    protected abstract void renderExtras();

    /** Click sobre un elemento del grid de contenido. */
    protected abstract void onItemClick(InventoryClickEvent event, int absoluteIndex);

    /** Click fuera del grid de contenido (botones fijos). */
    protected abstract void onExtraClick(InventoryClickEvent event);

    @Override
    public void build() {

        clear();
        slotToIndex.clear();

        int start = page * contentSlotsPerPage;
        int total = totalItemCount();

        for (int i = 0; i < contentSlotsPerPage; i++) {

            int absoluteIndex = start + i;
            if (absoluteIndex >= total) {
                break;
            }

            renderItem(i, absoluteIndex);
            slotToIndex.put(i, absoluteIndex);
        }

        renderExtras();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        int slot = event.getSlot();

        if (slot < contentSlotsPerPage && slotToIndex.containsKey(slot)) {
            onItemClick(event, slotToIndex.get(slot));
            return;
        }

        onExtraClick(event);
    }

    protected int maxPage() {
        return Math.max(0, (totalItemCount() - 1) / contentSlotsPerPage);
    }

    protected boolean hasNextPage() {
        return page < maxPage();
    }

    protected boolean hasPreviousPage() {
        return page > 0;
    }

    protected void nextPage() {
        if (hasNextPage()) {
            page++;
            build();
        }
    }

    protected void previousPage() {
        if (hasPreviousPage()) {
            page--;
            build();
        }
    }

}
