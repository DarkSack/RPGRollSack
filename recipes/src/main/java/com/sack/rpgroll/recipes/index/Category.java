package com.sack.rpgroll.recipes.index;

import com.sack.rpgroll.recipes.index.RecipeIndex.CatalogItem;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Set;

/**
 * Tipos del catálogo, para que no salga todo amontonado. Un ítem de un plugin cae en el tipo de su
 * material base (una espada de RPGRoll es un arma); además todos los que no son vanilla puros salen
 * también en {@link #SERVER}. {@link #OWN} junta solo lo que se fabrica con recetas de los plugins
 * propios (RPGRoll y Sackito), sin las materias primas que nada produce.
 */
public enum Category {

    ALL(Material.KNOWLEDGE_BOOK),
    SERVER(Material.NETHER_STAR),
    OWN(Material.SMITHING_TABLE),
    WEAPONS(Material.IRON_SWORD),
    TOOLS(Material.IRON_PICKAXE),
    ARMOR(Material.IRON_CHESTPLATE),
    FOOD(Material.BREAD),
    POTIONS(Material.POTION),
    REDSTONE(Material.REDSTONE),
    BLOCKS(Material.BRICKS),
    MATERIALS(Material.IRON_INGOT);

    private static final Set<Material> POTION_LIKE = Set.of(Material.POTION, Material.SPLASH_POTION,
            Material.LINGERING_POTION, Material.HONEY_BOTTLE, Material.ENCHANTED_BOOK, Material.EXPERIENCE_BOTTLE,
            Material.TIPPED_ARROW, Material.DRAGON_BREATH, Material.GLASS_BOTTLE, Material.BREWING_STAND);
    private static final Set<Material> WEAPON = Set.of(Material.BOW, Material.CROSSBOW, Material.TRIDENT,
            Material.MACE, Material.ARROW, Material.SPECTRAL_ARROW, Material.FIRE_CHARGE, Material.WIND_CHARGE);
    private static final Set<Material> TOOL = Set.of(Material.SHEARS, Material.FISHING_ROD, Material.FLINT_AND_STEEL,
            Material.BRUSH, Material.COMPASS, Material.RECOVERY_COMPASS, Material.CLOCK, Material.SPYGLASS,
            Material.LEAD, Material.NAME_TAG, Material.CARROT_ON_A_STICK, Material.WARPED_FUNGUS_ON_A_STICK,
            Material.MAP, Material.FILLED_MAP, Material.BUNDLE);
    private static final Set<Material> ARMOR_PIECE = Set.of(Material.SHIELD, Material.ELYTRA,
            Material.TURTLE_HELMET, Material.WOLF_ARMOR, Material.CARVED_PUMPKIN);
    private static final Set<Material> REDSTONE_PART = Set.of(Material.PISTON, Material.STICKY_PISTON,
            Material.REPEATER, Material.COMPARATOR, Material.OBSERVER, Material.HOPPER, Material.DROPPER,
            Material.DISPENSER, Material.LEVER, Material.TARGET, Material.DAYLIGHT_DETECTOR, Material.TRIPWIRE_HOOK,
            Material.CRAFTER, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR, Material.TNT,
            Material.MINECART, Material.LIGHTNING_ROD);

    private final Material icon;

    Category(Material icon) {
        this.icon = icon;
    }

    public Material icon() {
        return icon;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean matches(CatalogItem item) {
        return switch (this) {
            case ALL -> true;
            case SERVER -> !item.plain();
            case OWN -> item.sources().stream().anyMatch(Category::ownSource);
            default -> of(item.stack().getType()) == this;
        };
    }

    /** Un origen de recetas propio: los plugins RPGRoll-* y Sackito*. */
    public static boolean ownSource(String source) {
        String id = source.toLowerCase(Locale.ROOT);
        return id.startsWith("rpgroll") || id.startsWith("sackito");
    }

    /** El tipo de un material; los especiales (todo, del servidor, propios) nunca salen de aquí. */
    @SuppressWarnings("deprecation") // Material#isEdible: sin alternativa estable en la API de 26.1
    public static Category of(Material material) {
        String name = material.name();
        if (POTION_LIKE.contains(material)) {
            return POTIONS;
        }
        if (WEAPON.contains(material) || name.endsWith("_SWORD") || name.endsWith("_SPEAR")) {
            return WEAPONS;
        }
        if (TOOL.contains(material) || name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE") || name.endsWith("BUCKET")) {
            return TOOLS;
        }
        if (ARMOR_PIECE.contains(material) || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.endsWith("_HORSE_ARMOR")) {
            return ARMOR;
        }
        if (material.isEdible()) {
            return FOOD;
        }
        if (REDSTONE_PART.contains(material) || name.contains("REDSTONE") || name.endsWith("_BUTTON")
                || name.endsWith("_PRESSURE_PLATE") || name.endsWith("RAIL") || name.endsWith("_MINECART")) {
            return REDSTONE;
        }
        return material.isBlock() ? BLOCKS : MATERIALS;
    }
}
