package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.InventoryGUI;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

/** Base para GUIs paginadas — mismo patrón usado en el resto de los addons de RPGRoll. */
public abstract class PaginatedGUI extends InventoryGUI {

    protected final int contentSlotsPerPage;
    protected int page = 0;

    private final Map<Integer, Integer> slotToIndex = new HashMap<>();

    protected PaginatedGUI(Player player, Component title, int size, int contentSlotsPerPage) {
        super(player, title, size);
        this.contentSlotsPerPage = contentSlotsPerPage;
    }

    protected abstract int totalItemCount();

    protected abstract void renderItem(int contentSlot, int absoluteIndex);

    protected abstract void renderExtras();

    protected abstract void onItemClick(InventoryClickEvent event, int absoluteIndex);

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
