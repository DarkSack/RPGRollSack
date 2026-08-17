package com.sack.rpgroll.mobs.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobCategory;
import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.gui.editor.MobEditorHubGUI;
import com.sack.rpgroll.mobs.gui.editor.MobEditorSession;
import com.sack.rpgroll.mobs.registry.MobStatRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Navegador de mobs: grid paginado, filtro por categoría, y búsqueda por
 * nombre/id. Click abre el editor gráfico completo de esa definición.
 */
public class MobBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 54;
    private static final int CONTENT_SLOTS = 36;

    private static final int CATEGORY_ROW_START = 36;
    private static final int MAX_CATEGORY_BUTTONS = 8;

    private static final int PREV_SLOT = 45;
    private static final int NEW_SLOT = 46;
    private static final int SEARCH_SLOT = 48;
    private static final int ALL_CATEGORIES_SLOT = 49;
    private static final int CLEAR_SEARCH_SLOT = 50;
    private static final int NEXT_SLOT = 53;

    private final MobManager mobManager;
    private final MobStatRegistry statRegistry;
    private final ChatPromptManager chatPromptManager;
    private final Plugin plugin;
    private final LangManager lang;

    private List<MobDefinition> filtered;
    private List<MobCategory> categories;
    private MobCategory activeCategory;
    private String searchText = "";

    public MobBrowserGUI(Player player, MobManager mobManager, MobStatRegistry statRegistry,
            ChatPromptManager chatPromptManager, Plugin plugin) {

        super(player, chatPromptManager.lang().component("gui.browser.title"), SIZE, CONTENT_SLOTS);

        this.mobManager = mobManager;
        this.statRegistry = statRegistry;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;
        this.lang = chatPromptManager.lang();

        recomputeCategories();
        applyFilters();
    }

    private void recomputeCategories() {

        Set<MobCategory> unique = new LinkedHashSet<>();
        for (MobDefinition definition : mobManager.getAll()) {
            unique.add(definition.category());
        }

        categories = new ArrayList<>(unique);
    }

    private void applyFilters() {

        String search = searchText.toLowerCase(Locale.ROOT);

        filtered = mobManager.getAll().stream()
                .filter(def -> activeCategory == null || def.category() == activeCategory)
                .filter(def -> search.isBlank()
                        || def.id().toLowerCase(Locale.ROOT).contains(search)
                        || def.displayName().toLowerCase(Locale.ROOT).contains(search))
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList();

        page = 0;
        build();
    }

    @Override
    protected int totalItemCount() {
        return filtered.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        MobDefinition definition = filtered.get(absoluteIndex);

        setItem(contentSlot, new ItemBuilder(iconFor(definition))
                .setName(ComponentUtils.parse(definition.displayName())
                        .colorIfAbsent(NamedTextColor.WHITE))
                .setLore(
                        Component.text(definition.id(), NamedTextColor.DARK_GRAY),
                        lang.component("gui.browser.item_category_level", "category", definition.category(),
                                "level", definition.level()),
                        lang.component("gui.common.click_to_edit"))
                .build());
    }

    private Material iconFor(MobDefinition definition) {

        try {
            Material egg = Material.valueOf(definition.model().baseEntityType().toUpperCase(Locale.ROOT)
                    + "_SPAWN_EGG");
            return egg;
        } catch (IllegalArgumentException e) {
            return Material.ZOMBIE_HEAD;
        }
    }

    @Override
    protected void renderExtras() {

        for (int i = 0; i < categories.size() && i < MAX_CATEGORY_BUTTONS; i++) {

            MobCategory category = categories.get(i);
            boolean active = category == activeCategory;

            setItem(CATEGORY_ROW_START + i, new ItemBuilder(active ? Material.CHEST : Material.BARREL)
                    .setName(Component.text(category.name(), active ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                    .build());
        }

        setItem(PREV_SLOT, hasPreviousPage()
                ? new ItemBuilder(Material.ARROW).setName(lang.component("gui.common.prev_page")).build()
                : ItemBuilder.createFiller());

        setItem(NEXT_SLOT, hasNextPage()
                ? new ItemBuilder(Material.ARROW).setName(lang.component("gui.common.next_page")).build()
                : ItemBuilder.createFiller());

        setItem(SEARCH_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(lang.component("gui.browser.search_button"))
                .setLore(searchText.isBlank() ? lang.component("gui.browser.no_filter")
                        : lang.component("gui.browser.filter_label", "text", searchText))
                .build());

        setItem(ALL_CATEGORIES_SLOT, new ItemBuilder(activeCategory == null ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(lang.component("gui.browser.all_categories"))
                .build());

        setItem(CLEAR_SEARCH_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(lang.component("gui.browser.clear_search"))
                .build());

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.browser.create_new"))
                .build());
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {

        MobDefinition definition = filtered.get(absoluteIndex);
        MobEditorSession session = new MobEditorSession(definition, mobManager, statRegistry, chatPromptManager,
                plugin);

        new MobEditorHubGUI(player, session).open();
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        int slot = event.getSlot();

        if (slot >= CATEGORY_ROW_START
                && slot < CATEGORY_ROW_START + Math.min(categories.size(), MAX_CATEGORY_BUTTONS)) {
            activeCategory = categories.get(slot - CATEGORY_ROW_START);
            applyFilters();
            return;
        }

        switch (slot) {
            case PREV_SLOT -> previousPage();
            case NEXT_SLOT -> nextPage();
            case SEARCH_SLOT -> promptSearch();
            case NEW_SLOT -> promptNewMob();
            case ALL_CATEGORIES_SLOT -> {
                activeCategory = null;
                applyFilters();
            }
            case CLEAR_SEARCH_SLOT -> {
                searchText = "";
                applyFilters();
            }
            default -> {
            }
        }
    }

    private void promptSearch() {
        chatPromptManager.prompt(player, "gui.browser.prompt_search", value -> {
            searchText = value.trim();
            applyFilters();
        });
    }

    private void promptNewMob() {
        chatPromptManager.prompt(player, "gui.browser.prompt_new_mob", value -> {

            String[] parts = value.split(";", 2);
            String id = parts[0].trim().toLowerCase(Locale.ROOT).replace(' ', '_');
            String baseEntityType = parts.length > 1 ? parts[1].trim().toUpperCase(Locale.ROOT) : "ZOMBIE";

            if (id.isBlank() || mobManager.exists(id)) {
                lang.send(player, "gui.browser.invalid_id");
                return;
            }

            MobDefinition definition = new MobDefinition(id, null, id, null, 1, null, null, null,
                    com.sack.rpgroll.mobs.core.MobModel.defaults(baseEntityType), null, null, null, null, null,
                    null, null, null, null, null, null, null);

            MobEditorSession session = new MobEditorSession(definition, mobManager, statRegistry, chatPromptManager,
                    plugin);

            new MobEditorHubGUI(player, session).open();
        });
    }

    /** Recarga la lista de categorías y aplica de nuevo los filtros — llamado al volver de un save. */
    public void refresh() {
        recomputeCategories();
        applyFilters();
    }

}
