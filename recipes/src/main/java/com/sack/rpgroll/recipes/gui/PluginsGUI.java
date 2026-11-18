package com.sack.rpgroll.recipes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.recipes.index.Category;
import com.sack.rpgroll.recipes.index.RecipeIndex;
import com.sack.rpgroll.recipes.index.RecipeIndex.CatalogItem;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Un apartado por cada plugin propio (RPGRoll-*, Sackito*) que fabrica algo: al abrirlo, el catálogo
 * ya filtrado por ese origen, con solo lo que sus recetas producen.
 */
final class PluginsGUI extends InventoryGUI {

    /** Fondo dibujado (sackito:menus/recetario_plugins): volver (27) y cerrar (35) van dibujados. */
    static final String BACKGROUND = "recetario_plugins";

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int BACK = 27;
    private static final int CLOSE = 35;

    private final Viewer viewer;
    private final LangManager lang;
    private final RecipeIndex index;
    private final List<String> shown = new ArrayList<>();

    PluginsGUI(Player player, Viewer viewer, RecipeIndex index) {
        super(player, RecipesMenus.skin().title(BACKGROUND, Buttons.text(viewer.lang().raw("gui.plugins.title"))), 36);
        this.viewer = viewer;
        this.lang = viewer.lang();
        this.index = index;
    }

    /** Los orígenes propios con lo que producen (en el orden del índice), sin los que no fabrican nada. */
    static Map<String, List<CatalogItem>> sections(RecipeIndex index, Player player) {
        Map<String, List<CatalogItem>> sections = new LinkedHashMap<>();
        for (String source : index.sources()) {
            if (Category.ownSource(source)) {
                List<CatalogItem> items = index.catalog(null, source, "", player);
                if (!items.isEmpty()) {
                    sections.put(source, items);
                }
            }
        }
        return sections;
    }

    /** Nombre del apartado: {@code gui.plugins.source.<origen>} del idioma o el origen sin "RPGRoll-". */
    static String label(LangManager lang, String source) {
        String key = "gui.plugins.source." + source.toLowerCase(Locale.ROOT);
        String found = lang.raw(key);
        return found.equals(key) ? shortName(source) : found;
    }

    static String shortName(String source) {
        String name = source.replaceFirst("(?i)^rpgroll[-_ ]?", "");
        return name.isBlank() ? source : name;
    }

    @Override
    public void build() {

        clear();
        shown.clear();
        for (int slot = 0; slot < 36; slot++) {
            setItem(slot, Buttons.filler());
        }
        int i = 0;
        for (var section : sections(index, player).entrySet()) {
            if (i >= SLOTS.length) {
                break;
            }
            shown.add(section.getKey());
            ItemStack icon = section.getValue().getFirst().icon();
            int count = section.getValue().size();
            String lore = lang.raw("gui.plugins.section.lore", "count", count,
                    "items", lang.raw(count == 1 ? "gui.plugins.section.one" : "gui.plugins.section.many"),
                    "source", section.getKey());
            // El ítem real (con su modelo) como icono, no solo su material base.
            setItem(SLOTS[i++], rename(icon, section.getKey(), Buttons.lines(lore)));
        }
        if (shown.isEmpty()) {
            setItem(13, Buttons.of(lang, Material.STRUCTURE_VOID, "gui.plugins.empty"));
        }
        setItem(BACK, RecipesMenus.skin().button(Buttons.of(lang, Material.ARROW, "gui.plugins.back")));
        setItem(CLOSE, RecipesMenus.skin().button(Buttons.of(lang, Material.BARRIER, "gui.close")));
    }

    private ItemStack rename(ItemStack icon, String source, List<Component> lore) {
        ItemStack copy = icon.clone();
        copy.setAmount(1);
        var meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }
        meta.displayName(Buttons.text(lang.raw("gui.plugins.section.name", "name", label(lang, source))));
        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        copy.setItemMeta(meta);
        return copy;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();
        for (int i = 0; i < SLOTS.length && i < shown.size(); i++) {
            if (SLOTS[i] == slot) {
                viewer.click(player);
                viewer.push(player, new CatalogGUI(player, viewer, index, "", Category.ALL, shown.get(i)));
                return;
            }
        }
        if (slot == BACK) {
            viewer.back(player);
        } else if (slot == CLOSE) {
            close();
        }
    }
}
