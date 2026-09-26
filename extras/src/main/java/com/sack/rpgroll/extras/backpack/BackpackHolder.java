package com.sack.rpgroll.extras.backpack;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Una mochila abierta: quién la mira, qué pestaña y, si está en el suelo, en
 * qué bloque. El inventario es uno solo; cambiar de pestaña cambia su
 * contenido, no el inventario.
 */
public class BackpackHolder implements InventoryHolder {

    private final Player viewer;
    private final BackpackData data;
    private final BackpackTier tier;
    private final Block block;
    private Inventory inventory;
    private int page;

    BackpackHolder(Player viewer, BackpackData data, BackpackTier tier, Block block) {
        this.viewer = viewer;
        this.data = data;
        this.tier = tier;
        this.block = block;
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player viewer() {
        return viewer;
    }

    public BackpackData data() {
        return data;
    }

    public BackpackTier tier() {
        return tier;
    }

    /** null si se abrió desde un ítem. */
    public Block block() {
        return block;
    }

    public int page() {
        return page;
    }

    void page(int page) {
        this.page = page;
    }

    public int pages() {
        return BackpackPages.pages(tier.slots());
    }

    /** Primera casilla de la fila de botones. */
    public int buttonRowStart() {
        return BackpackPages.contentRows(tier.slots()) * 9;
    }

    /** Si la casilla del inventario de arriba es un espacio usable de esta pestaña. */
    public boolean isContentSlot(int slot) {
        return slot >= 0 && slot < BackpackPages.slotsOnPage(tier.slots(), page);
    }

    /** Índice en el contenido total de la casilla {@code slot} de esta pestaña. */
    public int index(int slot) {
        return page * BackpackPages.PAGE_SIZE + slot;
    }

}
