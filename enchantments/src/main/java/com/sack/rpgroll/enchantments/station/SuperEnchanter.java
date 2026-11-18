package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.core.Rarity;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;
import com.sack.rpgroll.enchantments.station.EnchantPlan.Entry;
import com.sack.rpgroll.enchantments.station.EnchantPlan.Kind;
import com.sack.rpgroll.util.ComponentUtils;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * El Súper encantador: lista todos los encantamientos que admite el ítem de la mano (vanilla y del
 * plugin), el jugador elige el nivel de cada uno (subir, bajar o quitar) y paga el total en
 * experiencia y lapislázuli. Los vanilla pueden pasar de su máximo hasta el tope ({@link #cap}).
 * Sin libros, sin maldiciones y, salvo {@code treasure: true}, sin tesoros.
 */
public final class SuperEnchanter implements Listener {

    static final String VANILLA = "minecraft:";
    static final String CUSTOM = "rpgroll:";

    private final LangManager lang;
    private final EnchantmentManager manager;
    private final EnchantmentItem items;
    private final NamespacedKey recipeKey;
    private final StationBlocks blocks;
    private String menuBackground = "";

    private boolean enabled;
    private int extraLevels = 3;
    private int hardCap = 10;
    private int baseLevels = 15;
    private int stepLevels = 10;
    private int lapisPerLevel = 16;
    private Map<String, Integer> caps = Map.of();
    private Set<String> excluded = Set.of();
    private boolean treasure;
    private int vanillaLevels = 2;
    private int vanillaLapis = 3;
    private Map<Rarity, Integer> rarityLevels = defaults(3, 5, 8, 12, 18, 24, 30);
    private Map<Rarity, Integer> rarityLapis = defaults(4, 6, 10, 16, 24, 32, 48);

    public SuperEnchanter(Plugin plugin, LangManager lang, EnchantmentManager manager, EnchantmentItem items) {
        this.lang = lang;
        this.manager = manager;
        this.items = items;
        this.recipeKey = new NamespacedKey(plugin, "super_enchanter");
        this.blocks = new StationBlocks(plugin, "super_enchanter", Material.ENCHANTING_TABLE, this::item, this::open);
        Bukkit.getPluginManager().registerEvents(blocks, plugin);
    }

    public void configure(ConfigurationSection s) {
        Bukkit.removeRecipe(recipeKey);
        enabled = s == null || s.getBoolean("enabled", true);
        blocks.model(s == null ? "" : s.getString("model", ""));
        menuBackground = s == null ? "" : s.getString("menu-background", "");
        if (s != null) {
            extraLevels = Math.max(1, s.getInt("extra-levels", 3));
            hardCap = Math.max(2, Math.min(255, s.getInt("hard-cap", 10)));
            baseLevels = Math.max(0, s.getInt("base-levels", 15));
            stepLevels = Math.max(0, s.getInt("step-levels", 10));
            lapisPerLevel = Math.max(0, s.getInt("lapis-per-level", 16));
            Map<String, Integer> c = new HashMap<>();
            ConfigurationSection cs = s.getConfigurationSection("caps");
            if (cs != null) {
                cs.getKeys(false).forEach(k -> c.put(k.toLowerCase(Locale.ROOT), cs.getInt(k)));
            }
            caps = c;
            Set<String> ex = new HashSet<>();
            s.getStringList("excluded").forEach(e -> ex.add(e.toLowerCase(Locale.ROOT).replace("minecraft:", "")));
            excluded = ex;
            treasure = s.getBoolean("treasure", false);
            vanillaLevels = Math.max(0, s.getInt("vanilla-levels", 2));
            vanillaLapis = Math.max(0, s.getInt("vanilla-lapis", 3));
            rarityLevels = byRarity(s.getConfigurationSection("rarity-levels"), defaults(3, 5, 8, 12, 18, 24, 30));
            rarityLapis = byRarity(s.getConfigurationSection("rarity-lapis"), defaults(4, 6, 10, 16, 24, 32, 48));
        }
        if (!enabled) {
            return;
        }
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, item());
        recipe.shape("LNL", "DED", "OOO");
        recipe.setIngredient('L', Material.LAPIS_BLOCK);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('E', Material.ENCHANTING_TABLE);
        recipe.setIngredient('O', Material.CRYING_OBSIDIAN);
        Bukkit.addRecipe(recipe, true);
        Bukkit.getOnlinePlayers().forEach(p -> p.discoverRecipe(recipeKey));
    }

    public void shutdown() {
        Bukkit.removeRecipe(recipeKey);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled) {
            event.getPlayer().discoverRecipe(recipeKey);
        }
    }

    public ItemStack item() {
        ItemStack item = ItemStack.of(Material.ENCHANTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.component("station.super.item_name"));
        meta.lore(List.of(lang.component("station.super.item_lore_1"), lang.component("station.super.item_lore_2")));
        meta.getPersistentDataContainer().set(blocks.itemKey(), PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(!blocks.hasModel());
        blocks.decorate(meta);
        item.setItemMeta(meta);
        return item;
    }

    /** Hasta dónde se puede subir: el máximo vanilla más {@code extra-levels}, o el tope propio, sin pasar de hard-cap. */
    int cap(Enchantment enchantment) {
        int cap = caps.getOrDefault(enchantment.getKey().getKey(), enchantment.getMaxLevel() + extraLevels);
        return Math.min(hardCap, Math.max(enchantment.getMaxLevel(), cap));
    }

    /** Coste de llegar a {@code newLevel}: crece con cada nivel por encima del vanilla. */
    static int xpFor(int newLevel, int vanillaMax, int base, int step) {
        return base + step * Math.max(0, newLevel - vanillaMax - 1);
    }

    private static Map<Rarity, Integer> defaults(int... values) {
        Map<Rarity, Integer> out = new EnumMap<>(Rarity.class);
        for (Rarity r : Rarity.values()) {
            out.put(r, values[Math.min(r.ordinal(), values.length - 1)]);
        }
        return out;
    }

    private static Map<Rarity, Integer> byRarity(ConfigurationSection section, Map<Rarity, Integer> base) {
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    base.put(Rarity.valueOf(key.toUpperCase(Locale.ROOT)), Math.max(0, section.getInt(key)));
                } catch (IllegalArgumentException ignored) {
                    Bukkit.getLogger().warning("✘ RPGRoll-Enchantments: rareza desconocida en super-enchanter: " + key);
                }
            }
        }
        return base;
    }

    EnchantPlan.Prices prices() {
        return new EnchantPlan.Prices(vanillaLevels, vanillaLapis, baseLevels, stepLevels, lapisPerLevel,
                rarityLevels, rarityLapis);
    }

    private static boolean book(ItemStack item) {
        return item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK;
    }

    /** Todos los encantamientos que el ítem admite o ya lleva, con su nivel actual y su tope. */
    EnchantPlan plan(ItemStack item) {
        List<Entry> out = new ArrayList<>();
        if (item == null || item.getType().isAir() || book(item)) {
            return new EnchantPlan(out, prices());
        }
        List<Enchantment> all = new ArrayList<>();
        RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).forEach(all::add);
        Map<Enchantment, Integer> has = item.getEnchantments();
        for (Enchantment e : all) {
            int level = has.getOrDefault(e, 0);
            boolean editable = !e.isCursed() && (treasure || !e.isTreasure())
                    && !excluded.contains(e.getKey().getKey()) && e.canEnchantItem(item);
            if (!editable && level == 0) {
                continue;
            }
            Set<String> conflicts = new HashSet<>();
            for (Enchantment other : all) {
                if (!other.equals(e) && e.conflictsWith(other)) {
                    conflicts.add(VANILLA + other.getKey().getKey());
                }
            }
            out.add(new Entry(VANILLA + e.getKey().getKey(), Kind.VANILLA, level,
                    editable ? Math.max(level, cap(e)) : level, e.getMaxLevel(), Rarity.COMMON, conflicts, editable));
        }
        Map<String, Integer> custom = items.getAll(item);
        for (CustomEnchantment c : manager.getAll()) {
            int level = custom.getOrDefault(c.id(), 0);
            boolean editable = items.isCategoryAllowed(item, c);
            if (!editable && level == 0) {
                continue;
            }
            Set<String> conflicts = new HashSet<>();
            c.conflicts().forEach(id -> conflicts.add(CUSTOM + id.toLowerCase(Locale.ROOT)));
            out.add(new Entry(CUSTOM + c.id().toLowerCase(Locale.ROOT), Kind.CUSTOM, level,
                    editable ? Math.max(level, c.maxLevel()) : level, c.maxLevel(), c.rarity(), conflicts, editable));
        }
        return new EnchantPlan(out, prices());
    }

    /** Nombre para el menú: el vanilla traducido o el del plugin. */
    Component name(Entry entry) {
        if (entry.kind() == Kind.VANILLA) {
            Enchantment e = vanilla(entry.id());
            return e == null ? Component.text(entry.id()) : e.description();
        }
        return manager.get(customId(entry.id())).map(c -> ComponentUtils.parse(c.displayName()))
                .orElse(Component.text(entry.id()));
    }

    private static Enchantment vanilla(String id) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(id.substring(VANILLA.length())));
    }

    private String customId(String id) {
        String plain = id.substring(CUSTOM.length());
        return manager.getAll().stream().map(CustomEnchantment::id).filter(i -> i.equalsIgnoreCase(plain))
                .findFirst().orElse(plain);
    }

    private void open(Player player) {
        if (!enabled) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir() || book(held) || plan(held).editable().isEmpty()) {
            player.sendActionBar(lang.component("station.super.need_item"));
            player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, 0.6f);
            return;
        }
        new SuperEnchanterGUI(player, this, lang).open();
    }

    private static int lapisIn(Player player) {
        int n = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.LAPIS_LAZULI) {
                n += stack.getAmount();
            }
        }
        return n;
    }

    boolean canPay(Player player, EnchantPlan.Cost cost) {
        return player.getGameMode() == GameMode.CREATIVE
                || player.getLevel() >= cost.xp() && lapisIn(player) >= cost.lapis();
    }

    /** Aplica el plan al ítem de la mano y cobra el total. Devuelve la clave del mensaje. */
    String apply(Player player, ItemStack expected, EnchantPlan plan) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.isSimilar(expected)) {
            return "station.changed";
        }
        Map<String, Integer> changes = plan.changes();
        if (changes.isEmpty()) {
            return "station.super.no_changes";
        }
        EnchantPlan.Cost cost = plan.cost();
        if (!canPay(player, cost)) {
            return player.getLevel() < cost.xp() ? "station.no_xp" : "station.super.no_lapis";
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setLevel(player.getLevel() - cost.xp());
            player.getInventory().removeItem(ItemStack.of(Material.LAPIS_LAZULI, cost.lapis()));
        }
        ItemMeta meta = item.getItemMeta();
        changes.forEach((id, level) -> {
            Enchantment e = id.startsWith(VANILLA) ? vanilla(id) : null;
            if (e == null) {
                return;
            }
            if (level == 0) {
                meta.removeEnchant(e);
            } else {
                meta.addEnchant(e, level, true);
            }
        });
        item.setItemMeta(meta);
        // Primero lo que se quita o baja: así lo que se pone no choca con lo que se va.
        changes.entrySet().stream().filter(c -> c.getKey().startsWith(CUSTOM))
                .sorted(Map.Entry.comparingByValue())
                .forEach(c -> {
                    String id = customId(c.getKey());
                    if (c.getValue() == 0) {
                        items.remove(item, id);
                    } else {
                        manager.get(id).ifPresent(ench -> items.apply(item, ench, c.getValue()));
                    }
                });
        player.getInventory().setItemInMainHand(item);
        player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.3f);
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        return "station.super.done";
    }

    /** Fuente del fondo dibujado de su menú (vacía: el menú de siempre). */
    public String menuBackground() {
        return menuBackground;
    }
}
