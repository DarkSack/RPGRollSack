package com.sack.rpgroll.economy.auction;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Las secciones del buscador. Se decide por el ítem, no por lo que diga el
 * vendedor: un libro encantado siempre cae en libros aunque le cambie el nombre.
 */
public enum AuctionCategory {

    ALL(Material.NETHER_STAR),
    SPECIAL(Material.AMETHYST_SHARD),
    EQUIPMENT(Material.DIAMOND_SWORD),
    ARMOR(Material.DIAMOND_CHESTPLATE),
    BOOKS(Material.ENCHANTED_BOOK),
    POTIONS(Material.POTION),
    FOOD(Material.COOKED_BEEF),
    BLOCKS(Material.GRASS_BLOCK),
    MATERIALS(Material.IRON_INGOT);

    private static final NamespacedKey ITEMS_ID = new NamespacedKey("rpgroll-items", "item-id");

    private final Material icon;

    AuctionCategory(Material icon) {
        this.icon = icon;
    }

    public Material icon() {
        return icon;
    }

    public String langKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public AuctionCategory next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public AuctionCategory previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    public boolean matches(AuctionCategory category) {
        return this == ALL || this == category;
    }

    public static AuctionCategory of(ItemStack item) {

        boolean custom = item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(ITEMS_ID, PersistentDataType.STRING);

        Material type = item.getType();
        return classify(type.name(), custom, type.isEdible(), type.isBlock());
    }

    /** La regla, sin tocar el registro de Bukkit (para poder probarla). */
    static AuctionCategory classify(String material, boolean custom, boolean edible, boolean block) {

        if (custom) {
            return SPECIAL;
        }

        if (material.equals("ENCHANTED_BOOK") || material.equals("WRITTEN_BOOK") || material.equals("KNOWLEDGE_BOOK")) {
            return BOOKS;
        }

        if (material.endsWith("POTION") || material.equals("TIPPED_ARROW") || material.equals("OMINOUS_BOTTLE")) {
            return POTIONS;
        }

        if (material.endsWith("_HELMET") || material.endsWith("_CHESTPLATE") || material.endsWith("_LEGGINGS")
                || material.endsWith("_BOOTS") || material.equals("ELYTRA") || material.endsWith("HORSE_ARMOR")
                || material.equals("WOLF_ARMOR")) {
            return ARMOR;
        }

        if (material.endsWith("_SWORD") || material.endsWith("_AXE") || material.endsWith("_PICKAXE")
                || material.endsWith("_SHOVEL") || material.endsWith("_HOE") || material.endsWith("_SPEAR")
                || switch (material) {
                    case "BOW", "CROSSBOW", "TRIDENT", "MACE", "SHIELD", "FISHING_ROD", "SHEARS", "FLINT_AND_STEEL",
                            "BRUSH", "SPYGLASS", "COMPASS", "RECOVERY_COMPASS", "CLOCK", "LEAD",
                            "CARROT_ON_A_STICK", "WARPED_FUNGUS_ON_A_STICK", "TOTEM_OF_UNDYING" -> true;
                    default -> false;
                }) {
            return EQUIPMENT;
        }

        if (edible) {
            return FOOD;
        }

        return block ? BLOCKS : MATERIALS;
    }

}
