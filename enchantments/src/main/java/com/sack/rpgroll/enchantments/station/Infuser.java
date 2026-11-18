package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.enchantments.core.EnchantCategory;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;
import com.sack.rpgroll.enchantments.util.RomanNumerals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El Infusor: graba el efecto de una poción en una armadura (vanilla o de plugin). Mientras la llevas
 * puesta tienes ese efecto. Se usa con la pieza en la mano: clic derecho en el bloque, eliges una de
 * las pociones que llevas y se gasta la poción y la experiencia.
 * <p>
 * La infusión va en el PDC de la pieza ({@code efecto:amplificador;...}) y se ve en el lore, debajo
 * de los encantamientos de RPGRoll-Enchantments.
 */
public final class Infuser implements Listener {

    private static final int DURATION = 300;   // >200 ticks: la visión nocturna no parpadea

    private final Plugin plugin;
    private final LangManager lang;
    private final EnchantmentItem enchantments;
    private final NamespacedKey infusionKey;
    private final NamespacedKey recipeKey;
    private final StationBlocks blocks;
    private String menuBackground = "";
    private final Map<UUID, Set<PotionEffectType>> applied = new HashMap<>();

    private boolean enabled;
    private int costLevels = 10;
    private int maxPerPiece = 1;
    private int defaultMaxAmplifier = 1;
    private Map<String, Integer> maxAmplifier = Map.of("strength", 0, "regeneration", 0, "resistance", 0);
    private static final List<String> DEFAULT_ALLOWED = List.of("speed", "jump_boost", "strength", "regeneration",
            "resistance", "fire_resistance", "water_breathing", "night_vision", "slow_falling", "luck");
    private Set<String> allowed = Set.copyOf(DEFAULT_ALLOWED);
    private Map<String, Integer> defaultCaps() {
        return Map.of("strength", 0, "regeneration", 0, "resistance", 0);
    }

    public Infuser(Plugin plugin, LangManager lang, EnchantmentItem enchantments) {
        this.plugin = plugin;
        this.lang = lang;
        this.enchantments = enchantments;
        this.infusionKey = new NamespacedKey(plugin, "infusion");
        this.recipeKey = new NamespacedKey(plugin, "infuser");
        this.blocks = new StationBlocks(plugin, "infuser", Material.BREWING_STAND, this::item, this::open);
        Bukkit.getPluginManager().registerEvents(blocks, plugin);
        enchantments.extraLore(this::loreLines);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 40L, 40L);
    }

    public void configure(ConfigurationSection s) {
        Bukkit.removeRecipe(recipeKey);
        enabled = s == null || s.getBoolean("enabled", true);
        blocks.model(s == null ? "" : s.getString("model", ""));
        menuBackground = s == null ? "" : s.getString("menu-background", "");
        if (s == null) {
            maxAmplifier = defaultCaps();
        }
        if (s != null) {
            costLevels = Math.max(0, s.getInt("cost-levels", 10));
            maxPerPiece = Math.max(1, s.getInt("max-effects-per-piece", 1));
            Map<String, Integer> caps = new HashMap<>();
            ConfigurationSection amp = s.getConfigurationSection("max-amplifier");
            if (amp != null) {
                for (String key : amp.getKeys(false)) {
                    caps.put(key.toLowerCase(Locale.ROOT), Math.max(0, amp.getInt(key)));
                }
            }
            defaultMaxAmplifier = caps.getOrDefault("default", 1);
            maxAmplifier = caps;
            if (amp == null) {
                maxAmplifier = defaultCaps();
            }
            Set<String> list = new HashSet<>();
            List<String> configured = s.isList("allowed-effects") ? s.getStringList("allowed-effects") : DEFAULT_ALLOWED;
            configured.forEach(e -> list.add(e.toLowerCase(Locale.ROOT).replace("minecraft:", "")));
            allowed = list;
        }
        if (!enabled) {
            return;
        }
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, item());
        recipe.shape(" T ", "BSB", "DDD");
        recipe.setIngredient('T', Material.GHAST_TEAR);
        recipe.setIngredient('B', Material.BLAZE_ROD);
        recipe.setIngredient('S', Material.BREWING_STAND);
        recipe.setIngredient('D', Material.DIAMOND);
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
        ItemStack item = ItemStack.of(Material.BREWING_STAND);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.component("station.infuser.item_name"));
        meta.lore(List.of(lang.component("station.infuser.item_lore_1"), lang.component("station.infuser.item_lore_2")));
        meta.getPersistentDataContainer().set(blocks.itemKey(), PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(!blocks.hasModel());
        blocks.decorate(meta);
        item.setItemMeta(meta);
        return item;
    }

    // ----- Infusiones guardadas en la pieza -----

    static Map<String, Integer> parse(String raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(";")) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                try {
                    out.put(kv[0], Integer.parseInt(kv[1]));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    static String write(Map<String, Integer> infusions) {
        StringBuilder sb = new StringBuilder();
        infusions.forEach((k, v) -> sb.append(sb.isEmpty() ? "" : ";").append(k).append(':').append(v));
        return sb.toString();
    }

    public Map<String, Integer> infusions(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Map.of();
        }
        return parse(item.getItemMeta().getPersistentDataContainer().get(infusionKey, PersistentDataType.STRING));
    }

    private static PotionEffectType type(String key) {
        return Registry.EFFECT.get(NamespacedKey.minecraft(key));
    }

    private List<Component> loreLines(ItemMeta meta) {
        Map<String, Integer> infusions = parse(meta.getPersistentDataContainer().get(infusionKey, PersistentDataType.STRING));
        List<Component> lines = new ArrayList<>();
        infusions.forEach((key, amp) -> {
            PotionEffectType type = type(key);
            if (type != null) {
                lines.add(lang.component("station.infuser.lore_line", "effect", Component.translatable(type.translationKey()),
                        "level", RomanNumerals.of(amp + 1)).decoration(TextDecoration.ITALIC, false));
            }
        });
        return lines;
    }

    // ----- Elegir y aplicar -----

    public boolean isArmor(ItemStack item) {
        return EnchantmentItem.isOf(item, EnchantCategory.ARMOR) && item.getType() != Material.ELYTRA;
    }

    public int costLevels() {
        return costLevels;
    }

    public int maxPerPiece() {
        return maxPerPiece;
    }

    /** El efecto que daría la poción (el primero permitido) con su amplificador ya recortado, o null. */
    public PotionEffect effectOf(ItemStack potion) {
        if (potion == null || !(potion.getItemMeta() instanceof PotionMeta meta)) {
            return null;
        }
        List<PotionEffect> effects = new ArrayList<>(meta.getCustomEffects());
        if (meta.getBasePotionType() != null) {
            effects.addAll(meta.getBasePotionType().getPotionEffects());
        }
        for (PotionEffect effect : effects) {
            String key = effect.getType().getKey().getKey();
            if (allowed.contains(key)) {
                int cap = maxAmplifier.getOrDefault(key, defaultMaxAmplifier);
                return new PotionEffect(effect.getType(), DURATION, Math.min(effect.getAmplifier(), cap), true, false, true);
            }
        }
        return null;
    }

    private void open(Player player) {
        if (!enabled) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isArmor(held)) {
            player.sendActionBar(lang.component("station.infuser.need_armor"));
            player.playSound(player, Sound.BLOCK_BREWING_STAND_BREW, 0.4f, 0.6f);
            return;
        }
        new InfuserGUI(player, this, lang).open();
    }

    /** Aplica la poción de la casilla {@code potionSlot} a la pieza de la mano. Devuelve la clave del mensaje. */
    String infuse(Player player, ItemStack expectedArmor, int potionSlot, ItemStack expectedPotion) {
        ItemStack armor = player.getInventory().getItemInMainHand();
        ItemStack potion = player.getInventory().getItem(potionSlot);
        if (!armor.isSimilar(expectedArmor) || potion == null || !potion.isSimilar(expectedPotion)) {
            return "station.changed";
        }
        PotionEffect effect = effectOf(potion);
        if (effect == null) {
            return "station.infuser.not_allowed";
        }
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        if (!creative && player.getLevel() < costLevels) {
            return "station.no_xp";
        }
        Map<String, Integer> infusions = new LinkedHashMap<>(infusions(armor));
        String key = effect.getType().getKey().getKey();
        infusions.remove(key);
        while (infusions.size() >= maxPerPiece) {
            infusions.remove(infusions.keySet().iterator().next());
        }
        infusions.put(key, effect.getAmplifier());

        ItemMeta meta = armor.getItemMeta();
        meta.getPersistentDataContainer().set(infusionKey, PersistentDataType.STRING, write(infusions));
        armor.setItemMeta(meta);
        enchantments.redrawLore(armor);
        player.getInventory().setItemInMainHand(armor);

        if (!creative) {
            player.setLevel(player.getLevel() - costLevels);
            Material type = potion.getType();
            potion.setAmount(potion.getAmount() - 1);
            player.getInventory().setItem(potionSlot, potion.getAmount() <= 0 ? null : potion);
            if (type == Material.POTION) {
                player.getInventory().addItem(ItemStack.of(Material.GLASS_BOTTLE))
                        .values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
        }
        player.playSound(player, Sound.BLOCK_BREWING_STAND_BREW, 1f, 1.2f);
        tick(player);
        return "station.infuser.done";
    }

    // ----- Efectos mientras se lleva puesta -----

    private void tickAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            tick(player);
        }
    }

    private void tick(Player player) {
        Map<PotionEffectType, Integer> wanted = new HashMap<>();
        if (enabled) {
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                infusions(piece).forEach((key, amp) -> {
                    PotionEffectType type = type(key);
                    if (type != null && allowed.contains(key)) {
                        wanted.merge(type, Math.min(amp, maxAmplifier.getOrDefault(key, defaultMaxAmplifier)), Math::max);
                    }
                });
            }
        }
        Set<PotionEffectType> before = applied.getOrDefault(player.getUniqueId(), Set.of());
        for (PotionEffectType gone : before) {
            if (!wanted.containsKey(gone)) {
                PotionEffect current = player.getPotionEffect(gone);
                // Solo el nuestro (ambiental y corto): una poción bebida se respeta.
                if (current != null && current.isAmbient() && current.getDuration() <= DURATION) {
                    player.removePotionEffect(gone);
                }
            }
        }
        wanted.forEach((type, amp) -> {
            PotionEffect current = player.getPotionEffect(type);
            if (current == null || current.getAmplifier() < amp
                    || (current.getAmplifier() == amp && current.isAmbient() && current.getDuration() < DURATION - 40)) {
                player.addPotionEffect(new PotionEffect(type, DURATION, amp, true, false, true));
            }
        });
        if (wanted.isEmpty()) {
            applied.remove(player.getUniqueId());
        } else {
            applied.put(player.getUniqueId(), new HashSet<>(wanted.keySet()));
        }
    }

    Component describe(PotionEffect effect) {
        return Component.translatable(effect.getType().translationKey(), NamedTextColor.AQUA)
                .append(Component.text(" " + RomanNumerals.of(effect.getAmplifier() + 1), NamedTextColor.AQUA));
    }

    Component describe(String key, int amp) {
        PotionEffectType type = type(key);
        return type == null ? Component.text(key) : describe(new PotionEffect(type, 1, amp));
    }

    /** Fuente del fondo dibujado de su menú (vacía: el menú de siempre). */
    public String menuBackground() {
        return menuBackground;
    }
}
