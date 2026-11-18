package com.sack.rpgroll.recipes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.recipes.index.Category;
import com.sack.rpgroll.recipes.index.RecipeIndex;
import com.sack.rpgroll.recipes.index.RecipeIndex.CatalogItem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Portada del recetario: un botón por tipo, con cuántos objetos tiene cada uno. */
final class CategoriesGUI extends InventoryGUI {

    /** Fondo dibujado (sackito:menus/recetario_categorias): buscar (31) y cerrar (35) van dibujados; plugins (23) lleva hueco. */
    static final String BACKGROUND = "recetario_categorias";

    private static final Map<Integer, Category> SLOTS = Map.ofEntries(
            Map.entry(10, Category.WEAPONS), Map.entry(11, Category.TOOLS), Map.entry(12, Category.ARMOR),
            Map.entry(13, Category.FOOD), Map.entry(14, Category.POTIONS), Map.entry(15, Category.REDSTONE),
            Map.entry(16, Category.BLOCKS), Map.entry(20, Category.MATERIALS), Map.entry(21, Category.SERVER),
            Map.entry(22, Category.OWN), Map.entry(24, Category.ALL));
    private static final int PLUGINS = 23;
    private static final int SEARCH = 31;
    private static final int CLOSE = 35;

    private final Viewer viewer;
    private final LangManager lang;
    private final RecipeIndex index;

    CategoriesGUI(Player player, Viewer viewer, RecipeIndex index) {
        super(player, RecipesMenus.skin().title(BACKGROUND, Buttons.text(viewer.lang().raw("gui.categories.title"))), 36);
        this.viewer = viewer;
        this.lang = viewer.lang();
        this.index = index;
    }

    @Override
    public void build() {

        clear();
        List<CatalogItem> all = index.catalog(null, null, "", player);
        Map<Category, Integer> counts = new EnumMap<>(Category.class);
        for (CatalogItem item : all) {
            for (Category category : Category.values()) {
                if (category.matches(item)) {
                    counts.merge(category, 1, Integer::sum);
                }
            }
        }

        for (int slot = 0; slot < 36; slot++) {
            setItem(slot, Buttons.filler());
        }
        SLOTS.forEach((slot, category) -> setItem(slot, Buttons.of(lang, category.icon(),
                "gui.categories." + category.id(), "count", counts.getOrDefault(category, 0))));
        int sections = PluginsGUI.sections(index, player).size();
        setItem(PLUGINS, Buttons.of(lang, Material.BOOKSHELF, "gui.categories.plugins", "count", sections,
                "sections", lang.raw(sections == 1 ? "gui.categories.plugins.one" : "gui.categories.plugins.many")));
        setItem(SEARCH, RecipesMenus.skin().button(Buttons.of(lang, Material.OAK_SIGN, "gui.categories.search")));
        setItem(CLOSE, RecipesMenus.skin().button(Buttons.of(lang, Material.BARRIER, "gui.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();
        Category category = SLOTS.get(slot);
        if (category != null) {
            viewer.click(player);
            viewer.push(player, new CatalogGUI(player, viewer, index, "", category));
        } else if (slot == PLUGINS) {
            viewer.click(player);
            viewer.push(player, new PluginsGUI(player, viewer, index));
        } else if (slot == SEARCH) {
            viewer.prompt().ask(player, text -> {
                if (text == null || text.isBlank()) {
                    open();
                } else {
                    viewer.push(player, new CatalogGUI(player, viewer, index, text, Category.ALL));
                }
            });
        } else if (slot == CLOSE) {
            close();
        }
    }
}
