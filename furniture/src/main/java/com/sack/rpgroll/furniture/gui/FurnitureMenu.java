package com.sack.rpgroll.furniture.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.furniture.core.CarpenterRecipe;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.core.FurnitureManager;
import com.sack.rpgroll.furniture.core.FurnitureManager.Category;
import com.sack.rpgroll.furniture.core.FurnitureVariant;
import com.sack.rpgroll.furniture.item.FurnitureItems;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * El catálogo de muebles, en dos modos:
 * <ul>
 *   <li>{@link Mode#CARPENTER}: un carpintero. Solo lo que tiene receta en su estación, con el
 *       coste a la vista; clic fabrica uno, clic con mayúsculas hasta diez.</li>
 *   <li>{@link Mode#CATALOG}: /muebles. Todo, con cómo se consigue; un admin se lleva el que
 *       pulse.</li>
 * </ul>
 * Tres pantallas: categorías → muebles de una categoría → versiones de un mueble.
 */
public class FurnitureMenu extends InventoryGUI {

    public enum Mode {
        CARPENTER,
        CATALOG
    }

    private static final int SIZE = 54;
    private static final int PAGE = 45;
    private static final int PREV = 45;
    private static final int BACK = 49;
    private static final int NEXT = 53;

    private final FurnitureManager manager;
    private final FurnitureItems items;
    private final LangManager lang;
    private final Mode mode;
    private final String station;
    private final boolean admin;

    private String category;
    private FurnitureDefinition selected;
    private int page;
    private final List<Runnable> actions = new ArrayList<>(java.util.Collections.nCopies(SIZE, null));
    private final List<java.util.function.Consumer<Boolean>> craftActions =
            new ArrayList<>(java.util.Collections.nCopies(SIZE, null));

    public FurnitureMenu(Player player, FurnitureManager manager, FurnitureItems items, LangManager lang, Mode mode,
            String station) {
        super(player, lang.component(mode == Mode.CARPENTER ? "gui.carpenter.title" : "gui.catalog.title",
                "station", station == null ? "" : lang.raw("station." + station)), SIZE);
        this.manager = manager;
        this.items = items;
        this.lang = lang;
        this.mode = mode;
        this.station = station == null ? CarpenterRecipe.DEFAULT_STATION : station;
        this.admin = player.hasPermission("rpgroll.furniture.admin");
    }

    private Predicate<FurnitureDefinition> visible() {
        return mode == Mode.CATALOG ? def -> true : def -> craftable(def) != null;
    }

    /** La primera receta de esta estación (del mueble o de alguna versión). */
    private CarpenterRecipe craftable(FurnitureDefinition def) {

        if (def.variants().isEmpty()) {
            return def.recipe(null).filter(r -> r.station().equals(station)).orElse(null);
        }
        for (String variant : def.variants().keySet()) {
            CarpenterRecipe recipe = def.recipe(variant).filter(r -> r.station().equals(station)).orElse(null);
            if (recipe != null) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    public void build() {

        clear();
        java.util.Collections.fill(actions, null);
        java.util.Collections.fill(craftActions, null);

        for (int slot = PAGE; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<ItemStack> entries = new ArrayList<>();
        List<Runnable> entryActions = new ArrayList<>();
        List<java.util.function.Consumer<Boolean>> entryCrafts = new ArrayList<>();

        if (selected != null) {
            buildVariants(entries, entryActions, entryCrafts);
        } else if (category != null) {
            buildFurniture(entries, entryActions, entryCrafts);
        } else {
            buildCategories(entries, entryActions, entryCrafts);
        }

        int pages = Math.max(1, (entries.size() + PAGE - 1) / PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        for (int i = 0; i < PAGE && page * PAGE + i < entries.size(); i++) {
            int index = page * PAGE + i;
            setItem(i, entries.get(index));
            actions.set(i, entryActions.get(index));
            craftActions.set(i, entryCrafts.get(index));
        }

        if (entries.isEmpty()) {
            setItem(22, button(Material.BARRIER, lang.component("gui.empty"), List.of()));
        }

        if (page > 0) {
            setItem(PREV, button(Material.ARROW, lang.component("gui.previous"), List.of()));
            actions.set(PREV, () -> {
                page--;
                build();
            });
        }
        if (page < pages - 1) {
            setItem(NEXT, button(Material.ARROW, lang.component("gui.next"), List.of()));
            actions.set(NEXT, () -> {
                page++;
                build();
            });
        }
        if (category != null) {
            setItem(BACK, button(Material.OAK_DOOR, lang.component("gui.back"), List.of()));
            actions.set(BACK, () -> {
                if (selected != null) {
                    selected = null;
                } else {
                    category = null;
                }
                page = 0;
                build();
            });
        } else {
            setItem(BACK, button(Material.BOOK, lang.component(mode == Mode.CARPENTER ? "gui.carpenter.help"
                    : "gui.catalog.help"), List.of()));
        }
    }

    private void buildCategories(List<ItemStack> entries, List<Runnable> entryActions,
            List<java.util.function.Consumer<Boolean>> entryCrafts) {

        for (Category c : manager.categoriesWith(visible())) {
            int count = manager.inCategory(c.id(), visible()).size();
            entries.add(button(c.icon(), ComponentUtils.parse(c.name()),
                    List.of(lang.component("gui.category.count", "count", count))));
            entryActions.add(() -> {
                category = c.id();
                page = 0;
                build();
            });
            entryCrafts.add(null);
        }
    }

    private void buildFurniture(List<ItemStack> entries, List<Runnable> entryActions,
            List<java.util.function.Consumer<Boolean>> entryCrafts) {

        for (FurnitureDefinition def : manager.inCategory(category, visible())) {

            List<String> variants = variantsFor(def);
            ItemStack icon = items.create(def, variants.isEmpty() ? null : variants.getFirst(), 1);
            List<Component> extra = new ArrayList<>();

            if (variants.size() > 1) {
                extra.add(lang.component("gui.variants", "count", variants.size()));
                entries.add(withExtraLore(icon, extra));
                entryActions.add(() -> {
                    selected = def;
                    page = 0;
                    build();
                });
                entryCrafts.add(null);
            } else {
                String variant = variants.isEmpty() ? null : variants.getFirst();
                extra.addAll(costLines(def, variant));
                entries.add(withExtraLore(icon, extra));
                entryActions.add(null);
                entryCrafts.add(shift -> use(def, variant, shift));
            }
        }
    }

    private void buildVariants(List<ItemStack> entries, List<Runnable> entryActions,
            List<java.util.function.Consumer<Boolean>> entryCrafts) {

        for (String variant : variantsFor(selected)) {
            ItemStack icon = items.create(selected, variant, 1);
            entries.add(withExtraLore(icon, costLines(selected, variant)));
            entryActions.add(null);
            FurnitureDefinition def = selected;
            entryCrafts.add(shift -> use(def, variant, shift));
        }
    }

    /** Las versiones que se muestran: en el carpintero, solo las que tienen receta aquí. */
    private List<String> variantsFor(FurnitureDefinition def) {

        List<String> out = new ArrayList<>();
        for (FurnitureVariant v : def.variants().values()) {
            if (mode == Mode.CATALOG || def.recipe(v.id()).filter(r -> r.station().equals(station)).isPresent()) {
                out.add(v.id());
            }
        }
        return out;
    }

    private List<Component> costLines(FurnitureDefinition def, String variant) {

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());

        CarpenterRecipe recipe = def.recipe(variant).orElse(null);
        if (recipe == null) {
            lines.add(lang.component("gui.catalog.not_craftable"));
        } else {
            if (mode == Mode.CATALOG) {
                lines.add(lang.component("gui.catalog.made_at", "station", lang.raw("station." + recipe.station())));
            }
            lines.add(lang.component("gui.cost.title"));
            for (Map.Entry<Material, Integer> entry : recipe.materials().entrySet()) {
                int have = Carpentry.count(player.getInventory(), entry.getKey());
                lines.add(lang.component(have >= entry.getValue() ? "gui.cost.material_ok" : "gui.cost.material_missing",
                        "amount", entry.getValue(), "material", entry.getKey().translationKey(),
                        "have", have));
            }
            if (recipe.money() > 0) {
                lines.add(lang.component("gui.cost.money", "money", formatMoney(recipe.money())));
            }
            if (recipe.amount() > 1) {
                lines.add(lang.component("gui.cost.amount", "amount", recipe.amount()));
            }
        }

        lines.add(Component.empty());
        if (mode == Mode.CARPENTER) {
            lines.add(lang.component("gui.carpenter.click"));
        } else if (admin) {
            lines.add(lang.component("gui.catalog.admin_take"));
        }
        return lines;
    }

    private void use(FurnitureDefinition def, String variant, boolean shift) {

        if (mode == Mode.CATALOG) {
            if (admin) {
                player.getInventory().addItem(items.create(def, variant, shift ? 16 : 1));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
            }
            return;
        }

        CarpenterRecipe recipe = def.recipe(variant).orElse(null);
        if (recipe == null) {
            return;
        }

        int times = shift ? 10 : 1;
        int made = 0;
        Carpentry.Result result = Carpentry.Result.OK;

        while (made < times) {
            result = Carpentry.craft(player, def, variant, recipe, items);
            if (result != Carpentry.Result.OK) {
                break;
            }
            made++;
        }

        if (made > 0) {
            player.playSound(player.getLocation(), Sound.UI_STONECUTTER_TAKE_RESULT, 0.8f, 1.0f);
            lang.send(player, "carpenter.crafted", "amount", made * recipe.amount(), "name", def.displayName(variant));
        }
        if (result != Carpentry.Result.OK) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
            lang.send(player, "carpenter." + result.name().toLowerCase());
        }
        build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) {
            return;
        }

        Runnable action = actions.get(slot);
        if (action != null) {
            action.run();
            return;
        }

        java.util.function.Consumer<Boolean> craft = craftActions.get(slot);
        if (craft != null) {
            craft.accept(event.isShiftClick());
        }
    }

    // ---------------------------------------------------------------- ítems de menú

    private static ItemStack button(Material material, Component name, List<Component> lore) {
        return new ItemBuilder(material, 1)
                .setName(plain(name))
                .setLore(lore.stream().map(FurnitureMenu::plain).toList())
                .build();
    }

    private static ItemStack withExtraLore(ItemStack item, List<Component> extra) {

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        extra.forEach(line -> lore.add(plain(line)));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static Component plain(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static String formatMoney(double money) {
        return com.sack.rpgroll.common.integration.VaultEconomy.get()
                .map(e -> e.format(money)).orElse(String.format(java.util.Locale.ROOT, "%.0f", money));
    }
}
