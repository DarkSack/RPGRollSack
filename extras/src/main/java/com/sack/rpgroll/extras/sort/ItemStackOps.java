package com.sack.rpgroll.extras.sort;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** {@link InventorySorter.Ops} para ítems reales; el grupo sale del material. */
final class ItemStackOps implements InventorySorter.Ops<ItemStack> {

    static final ItemStackOps INSTANCE = new ItemStackOps();

    @Override
    public boolean empty(ItemStack item) {
        return item == null || item.isEmpty();
    }

    @Override
    public int group(ItemStack item) {
        return group(item.getType());
    }

    /** 0 bloques, 1 herramientas, 2 armas y armadura, 3 comida, 4 menas y materiales, 5 el resto. */
    static int group(Material material) {
        String name = material.name();
        if (name.endsWith("_ORE") || name.startsWith("RAW_") || name.endsWith("_INGOT") || name.endsWith("_NUGGET")
                || name.endsWith("_SCRAP") || switch (material) {
                    case COAL, CHARCOAL, DIAMOND, EMERALD, LAPIS_LAZULI, REDSTONE, QUARTZ, AMETHYST_SHARD, STICK,
                            STRING, LEATHER, FEATHER, FLINT, GUNPOWDER, BONE, SLIME_BALL, BLAZE_ROD, ENDER_PEARL,
                            PAPER, BOOK, GLOWSTONE_DUST, PRISMARINE_SHARD, PRISMARINE_CRYSTALS -> true;
                    default -> false;
                }) {
            return 4;
        }
        if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_AXE")
                || switch (material) {
                    case SHEARS, FISHING_ROD, FLINT_AND_STEEL, BRUSH, SPYGLASS, COMPASS, CLOCK, BUCKET, LEAD -> true;
                    default -> false;
                }) {
            return 1;
        }
        if (name.endsWith("_SWORD") || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.endsWith("ARROW")
                || switch (material) {
                    case BOW, CROSSBOW, TRIDENT, MACE, SHIELD, ELYTRA -> true;
                    default -> false;
                }) {
            return 2;
        }
        if (material.isEdible()) {
            return 3;
        }
        return material.isBlock() ? 0 : 5;
    }

    @Override
    public String name(ItemStack item) {
        return item.getType().name();
    }

    @Override
    public boolean similar(ItemStack a, ItemStack b) {
        return a.isSimilar(b);
    }

    @Override
    public int amount(ItemStack item) {
        return item.getAmount();
    }

    @Override
    public int max(ItemStack item) {
        return item.getMaxStackSize();
    }

    @Override
    public ItemStack withAmount(ItemStack item, int amount) {
        ItemStack copy = item.clone();
        copy.setAmount(amount);
        return copy;
    }
}
