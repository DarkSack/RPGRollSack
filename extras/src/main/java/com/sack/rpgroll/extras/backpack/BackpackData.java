package com.sack.rpgroll.extras.backpack;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** El contenido de una mochila y a quién está ligada. */
public class BackpackData {

    private final UUID id;
    private UUID owner;
    private String ownerName;
    private ItemStack[] items;

    public BackpackData(UUID id, UUID owner, String ownerName, ItemStack[] items) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.items = items;
    }

    public UUID id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public boolean isBound() {
        return owner != null;
    }

    public void bind(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
    }

    public void unbind() {
        this.owner = null;
        this.ownerName = null;
    }

    public ItemStack[] items() {
        return items;
    }

    public ItemStack get(int index) {
        return index < items.length ? items[index] : null;
    }

    public void set(int index, ItemStack item) {
        if (index >= items.length) {
            items = Arrays.copyOf(items, index + 1);
        }
        items[index] = item == null || item.getType().isAir() ? null : item;
    }

    /**
     * Ajusta el contenido a {@code capacity} espacios. Si la mochila perdió
     * espacios (bajaron los slots de su nivel en la config), lo que quedó
     * fuera se acomoda en los huecos libres; lo que no cabe se devuelve.
     */
    public List<ItemStack> fit(int capacity) {

        List<ItemStack> outside = new ArrayList<>();

        for (int i = capacity; i < items.length; i++) {
            if (items[i] != null) {
                outside.add(items[i]);
            }
        }

        items = Arrays.copyOf(items, capacity);

        List<ItemStack> leftover = new ArrayList<>();
        int free = 0;

        for (ItemStack item : outside) {
            while (free < capacity && items[free] != null) {
                free++;
            }
            if (free < capacity) {
                items[free] = item;
            } else {
                leftover.add(item);
            }
        }

        return leftover;
    }

    public int used() {
        int used = 0;
        for (ItemStack item : items) {
            if (item != null) {
                used++;
            }
        }
        return used;
    }

}
