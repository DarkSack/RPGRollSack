package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.core.Rarity;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;
import com.sack.rpgroll.enchantments.util.RomanNumerals;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El Desencantador: Mayús + clic derecho en una muela con un ítem encantado en la mano. Quita el
 * encantamiento que elijas (vanilla o de RPGRoll) y te lo da en un libro; el resto se queda en el ítem.
 * Se paga con dinero (clic izquierdo, por Vault) o con esquirlas de olvidita (clic derecho), un ítem de
 * RPGRoll-Items que se reconoce por su id sin depender de ese plugin (él ya depende de este).
 */
public final class Disenchanter implements Listener {

    /** El id con que RPGRoll-Items marca sus ítems ({@code ItemInstanceService}). */
    private static final NamespacedKey ITEM_ID = NamespacedKey.fromString("rpgroll-items:item-id");

    /** Un encantamiento que se puede quitar: {@code key} es el id de RPGRoll o la clave vanilla. */
    record Option(String key, boolean vanilla, int level, boolean premium, Component name) {
    }

    private final LangManager lang;
    private final EnchantmentManager manager;
    private final EnchantmentItem enchantments;

    private boolean enabled = true;
    private double moneyPerLevel = 1500;
    private int shardsPerLevel = 1;
    private String shardItem = "esquirla_de_olvidita";

    public Disenchanter(LangManager lang, EnchantmentManager manager, EnchantmentItem enchantments) {
        this.lang = lang;
        this.manager = manager;
        this.enchantments = enchantments;
    }

    public void configure(ConfigurationSection s) {
        enabled = s == null || s.getBoolean("enabled", true);
        moneyPerLevel = s == null ? 1500 : Math.max(0, s.getDouble("money-per-level", 1500));
        shardsPerLevel = s == null ? 1 : Math.max(0, s.getInt("shards-per-level", 1));
        shardItem = s == null ? "esquirla_de_olvidita" : s.getString("shard-item", "esquirla_de_olvidita");
    }

    /** Lo que cuesta quitar un encantamiento de nivel {@code level}: doble si es tesoro o maldición. */
    static long price(double perLevel, int level, boolean premium) {
        return Math.round(perLevel * Math.max(1, level) * (premium ? 2 : 1));
    }

    /** Los de RPGRoll legendarios o más raros cuentan como tesoro. */
    static boolean premium(Rarity rarity) {
        return rarity != null && rarity.ordinal() >= Rarity.LEGENDARY.ordinal();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (!enabled || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || !event.getPlayer().isSneaking() || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.GRINDSTONE) {
            return;
        }
        Player player = event.getPlayer();
        event.setCancelled(true);
        if (options(player.getInventory().getItemInMainHand()).isEmpty()) {
            player.sendActionBar(lang.component("station.disenchanter.need_item"));
            player.playSound(player, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.6f);
            return;
        }
        new DisenchanterGUI(player, this, lang).open();
    }

    List<Option> options(ItemStack item) {
        List<Option> out = new ArrayList<>();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return out;
        }
        for (Map.Entry<Enchantment, Integer> e : item.getItemMeta().getEnchants().entrySet()) {
            Enchantment enchantment = e.getKey();
            out.add(new Option(enchantment.getKey().toString(), true, e.getValue(),
                    enchantment.isTreasure() || enchantment.isCursed(),
                    enchantment.description().color(NamedTextColor.AQUA)
                            .append(Component.text(" " + RomanNumerals.of(e.getValue()), NamedTextColor.AQUA))));
        }
        enchantments.getAll(item).forEach((id, level) -> {
            Optional<CustomEnchantment> custom = manager.get(id);
            if (custom.isPresent()) {
                out.add(new Option(id, false, level, premium(custom.get().rarity()),
                        ComponentUtils.parse(custom.get().displayName())
                                .append(Component.text(" " + RomanNumerals.of(level), NamedTextColor.GRAY))));
            }
        });
        return out;
    }

    long moneyCost(Option option) {
        return price(moneyPerLevel, option.level(), option.premium());
    }

    int shardCost(Option option) {
        return (int) price(shardsPerLevel, option.level(), option.premium());
    }

    /**
     * Quita {@code option} del ítem de la mano y da el libro. {@code withShards}: paga con esquirlas en
     * vez de dinero. Devuelve la clave del mensaje.
     */
    String disenchant(Player player, ItemStack expected, Option option, boolean withShards) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.isSimilar(expected) || !options(item).contains(option)) {
            return "station.changed";
        }
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        if (!creative) {
            if (withShards) {
                if (countShards(player) < shardCost(option)) {
                    return "station.disenchanter.no_shards";
                }
            } else if (!DisenchantMoney.available()) {
                return "station.disenchanter.no_economy";
            } else if (!DisenchantMoney.has(player, moneyCost(option))) {
                return "station.disenchanter.no_money";
            }
        }

        ItemStack book = option.vanilla() ? vanillaBook(option) : customBook(option);
        if (book == null) {
            return "station.changed";
        }

        // Se cobra antes de tocar el ítem: si el pago falla, no se pierde nada.
        if (!creative) {
            if (withShards) {
                takeShards(player, shardCost(option));
            } else if (!DisenchantMoney.take(player, moneyCost(option))) {
                return "station.disenchanter.no_money";
            }
        }

        if (option.vanilla()) {
            ItemMeta meta = item.getItemMeta();
            meta.removeEnchant(vanilla(option.key()));
            item.setItemMeta(meta);
        } else {
            enchantments.remove(item, option.key());
        }
        enchantments.redrawLore(item);
        player.getInventory().setItemInMainHand(item);
        player.getInventory().addItem(book)
                .values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        player.playSound(player, Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        return "station.disenchanter.done";
    }

    private static Enchantment vanilla(String key) {
        NamespacedKey nk = NamespacedKey.fromString(key);
        return nk == null ? null : org.bukkit.Registry.ENCHANTMENT.get(nk);
    }

    private static ItemStack vanillaBook(Option option) {
        Enchantment enchantment = vanilla(option.key());
        if (enchantment == null) {
            return null;
        }
        ItemStack book = ItemStack.of(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, option.level(), true);
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack customBook(Option option) {
        return manager.get(option.key()).map(custom -> enchantments.createBook(custom, option.level())).orElse(null);
    }

    private boolean isShard(ItemStack stack) {
        return stack != null && stack.hasItemMeta() && ITEM_ID != null && shardItem.equals(
                stack.getItemMeta().getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING));
    }

    int countShards(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isShard(stack)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void takeShards(Player player, int amount) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        int left = amount;
        for (int i = 0; i < storage.length && left > 0; i++) {
            if (isShard(storage[i])) {
                int taken = Math.min(left, storage[i].getAmount());
                storage[i].setAmount(storage[i].getAmount() - taken);
                if (storage[i].getAmount() <= 0) {
                    storage[i] = null;
                }
                left -= taken;
            }
        }
        player.getInventory().setStorageContents(storage);
    }

    String formatMoney(long amount) {
        return DisenchantMoney.format(amount);
    }
}
