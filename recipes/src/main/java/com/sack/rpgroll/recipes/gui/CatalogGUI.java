package com.sack.rpgroll.recipes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.recipe.RecipeStation;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.recipes.index.RecipeIndex;
import com.sack.rpgroll.recipes.index.RecipeIndex.CatalogItem;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * El catálogo: todos los objetos que aparecen en alguna receta, con filtro por estación, por
 * origen y buscador. Clic izquierdo = cómo se hace, derecho = para qué sirve; lo mismo sobre un
 * ítem del propio inventario.
 */
final class CatalogGUI extends InventoryGUI {

    private static final int PAGE = 45;
    private static final int PREV = 45;
    private static final int STATION = 46;
    private static final int SOURCE = 47;
    private static final int SEARCH = 48;
    private static final int INFO = 49;
    private static final int HAND = 50;
    private static final int CLOSE = 52;
    private static final int NEXT = 53;

    private final Viewer viewer;
    private final LangManager lang;
    private final RecipeIndex index;
    private String query;
    private String station;
    private String source;
    private int page;
    private List<CatalogItem> items = List.of();

    CatalogGUI(Player player, Viewer viewer, RecipeIndex index, String query) {
        super(player, Buttons.text(viewer.lang().raw("gui.catalog.title")), 54);
        this.viewer = viewer;
        this.lang = viewer.lang();
        this.index = index;
        this.query = query == null ? "" : query;
    }

    @Override
    public void build() {

        clear();
        items = index.catalog(station, source, query, player);
        int pages = Math.max(1, (items.size() + PAGE - 1) / PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        int from = page * PAGE;
        for (int i = 0; i < PAGE && from + i < items.size(); i++) {
            setItem(i, decorate(items.get(from + i)));
        }

        ItemStack filler = Buttons.filler();
        for (int slot = PAGE; slot < 54; slot++) {
            setItem(slot, filler);
        }

        if (page > 0) {
            setItem(PREV, Buttons.of(lang, Material.ARROW, "gui.catalog.prev", "page", page, "pages", pages));
        }
        if (page < pages - 1) {
            setItem(NEXT, Buttons.of(lang, Material.ARROW, "gui.catalog.next", "page", page + 2, "pages", pages));
        }

        setItem(STATION, stationButton());
        setItem(SOURCE, Buttons.of(lang, Material.NAME_TAG, "gui.catalog.source",
                "value", source == null ? lang.raw("gui.catalog.all") : source));
        setItem(SEARCH, Buttons.of(lang, Material.OAK_SIGN, "gui.catalog.search",
                "value", query.isBlank() ? lang.raw("gui.catalog.none") : query));
        setItem(INFO, Buttons.of(lang, Material.KNOWLEDGE_BOOK, "gui.catalog.info",
                "items", items.size(), "recipes", index.recipes().size(), "page", page + 1, "pages", pages));
        setItem(HAND, Buttons.of(lang, Material.LEAD, "gui.catalog.hand"));
        setItem(CLOSE, Buttons.of(lang, Material.BARRIER, "gui.close"));

        if (items.isEmpty()) {
            setItem(22, Buttons.of(lang, Material.STRUCTURE_VOID, "gui.catalog.empty"));
        }
    }

    private ItemStack stationButton() {

        String value = lang.raw("gui.catalog.all");
        Material icon = Material.CRAFTING_TABLE;
        for (RecipeStation s : index.stations()) {
            if (s.id().equals(station)) {
                value = viewer.indexes().plainName(s);
                icon = s.icon().getType();
            }
        }
        return Buttons.of(lang, icon, "gui.catalog.station", "value", value);
    }

    private ItemStack decorate(CatalogItem item) {
        int make = index.howToMake(item.stack(), player).size();
        int uses = index.usesOf(item.stack(), player).size();
        List<Component> hint = new ArrayList<>();
        hint.add(Component.empty());
        hint.addAll(Buttons.lines(lang.raw("gui.catalog.item_hint", "make", make, "uses", uses,
                "source", item.origin())));
        return Buttons.withLore(item.stack(), hint);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        if (slot < PAGE) {
            int i = page * PAGE + slot;
            if (i < items.size()) {
                viewer.click(player);
                viewer.showRecipes(player, items.get(i).stack(),
                        click.isRightClick() ? Viewer.Mode.USES : Viewer.Mode.MAKE, true);
            }
            return;
        }

        switch (slot) {
            case PREV -> turn(-1);
            case NEXT -> turn(1);
            case STATION -> {
                station = cycle(index.stations().stream().map(RecipeStation::id).toList(), station, click);
                page = 0;
                refresh();
            }
            case SOURCE -> {
                source = cycle(index.sources(), source, click);
                page = 0;
                refresh();
            }
            case SEARCH -> {
                if (click.isRightClick()) {
                    query = "";
                    page = 0;
                    refresh();
                } else {
                    viewer.prompt().ask(player, text -> {
                        if (text != null) {
                            query = text;
                            page = 0;
                        }
                        open();
                    });
                }
            }
            case HAND -> viewer.showRecipes(player, player.getInventory().getItemInMainHand(),
                    click.isRightClick() ? Viewer.Mode.USES : Viewer.Mode.MAKE, true);
            case CLOSE -> close();
            default -> {
            }
        }
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && !clicked.getType().isAir()) {
            viewer.showRecipes(player, clicked, event.getClick().isRightClick() ? Viewer.Mode.USES : Viewer.Mode.MAKE,
                    true);
        }
    }

    private void turn(int delta) {
        page += delta;
        refresh();
    }

    private void refresh() {
        viewer.click(player);
        build();
    }

    /** Siguiente valor (clic izquierdo), anterior (derecho) o todos (con mayúsculas). */
    static String cycle(List<String> values, String current, ClickType click) {
        if (click.isShiftClick() || values.isEmpty()) {
            return null;
        }
        int at = current == null ? -1 : values.indexOf(current);
        int next = click.isRightClick() ? at - 1 : at + 1;
        if (next < -1) {
            next = values.size() - 1;
        }
        return next < 0 || next >= values.size() ? null : values.get(next);
    }
}
