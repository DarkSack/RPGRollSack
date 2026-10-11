package com.sack.rpgroll.recipes.book;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.recipes.Settings;
import com.sack.rpgroll.recipes.gui.Viewer;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

/**
 * El recetario como objeto: clic derecho con él en la mano abre el catálogo. Se consigue con
 * {@code /recetas libro}, al entrar por primera vez (si se configura) o crafteando un libro con
 * una mesa de crafteo.
 */
public final class RecipeBook implements Listener {

    private final LangManager lang;
    private final Viewer viewer;
    private final NamespacedKey marker;
    private final NamespacedKey recipeKey;
    private final NamespacedKey receivedKey;
    private Settings.Book settings;

    public RecipeBook(Plugin plugin, LangManager lang, Viewer viewer, Settings.Book settings) {
        this.lang = lang;
        this.viewer = viewer;
        this.settings = settings;
        this.marker = new NamespacedKey(plugin, "recipe_book");
        this.recipeKey = new NamespacedKey(plugin, "recipe_book");
        this.receivedKey = new NamespacedKey(plugin, "recipe_book_received");
    }

    public void settings(Settings.Book settings) {
        this.settings = settings;
        registerRecipe();
    }

    public ItemStack create() {
        ItemStack stack = ItemStack.of(settings.material());
        ItemMeta meta = stack.getItemMeta();
        meta.itemName(ComponentUtils.parse(lang.raw("book.name")));
        meta.lore(Arrays.stream(lang.raw("book.lore").split("\n"))
                .map(line -> ComponentUtils.parse(line).decorationIfAbsent(TextDecoration.ITALIC,
                        TextDecoration.State.FALSE))
                .toList());
        meta.setMaxStackSize(1);
        if (settings.glint()) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (settings.model() != null) {
            meta.setItemModel(settings.model());
        }
        meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean is(ItemStack stack) {
        return stack != null && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(marker, PersistentDataType.BYTE);
    }

    public void give(Player player) {
        player.getInventory().addItem(create()).values()
                .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
    }

    /** Libro + mesa de crafteo = recetario (queda a la vista en el propio recetario). */
    public void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);
        if (settings.recipe()) {
            ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, create());
            recipe.addIngredient(Material.BOOK);
            recipe.addIngredient(Material.CRAFTING_TABLE);
            Bukkit.addRecipe(recipe);
        }
    }

    public void unregisterRecipe() {
        Bukkit.removeRecipe(recipeKey);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!is(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("rpgrollrecipes.use")) {
            lang.send(player, "msg.no_permission");
            return;
        }
        // Sobre un bloque que se fabrica se abre su receta; al aire o sobre cualquier otro, el catálogo.
        Block block = event.getClickedBlock();
        Material made = block == null ? Material.AIR : block.getBlockData().getPlacementMaterial();
        if (made.isAir() || viewer.indexes().current().howToMake(new ItemStack(made), player).isEmpty()) {
            viewer.openCatalog(player, "");
        } else {
            viewer.showRecipes(player, new ItemStack(made), Viewer.Mode.MAKE, false);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!settings.giveOnFirstJoin()
                || player.getPersistentDataContainer().has(receivedKey, PersistentDataType.BYTE)) {
            return;
        }
        player.getPersistentDataContainer().set(receivedKey, PersistentDataType.BYTE, (byte) 1);
        give(player);
    }

    /** El recetario no sirve de libro normal (librerías, libro y pluma...). */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe instanceof org.bukkit.Keyed keyed && keyed.getKey().equals(recipeKey)) {
            return;
        }
        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (is(stack)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
