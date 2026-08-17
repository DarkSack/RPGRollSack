package com.sack.rpgroll.dungeons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonManager;
import com.sack.rpgroll.dungeons.gui.editor.DungeonEditorHubGUI;
import com.sack.rpgroll.dungeons.gui.editor.DungeonEditorSession;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Navegador de mazmorras: grid paginado, filtro por categoría, búsqueda. Click abre el editor completo. */
public class DungeonBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 54;
    private static final int CONTENT_SLOTS = 36;

    private static final int CATEGORY_ROW_START = 36;
    private static final int MAX_CATEGORY_BUTTONS = 8;

    private static final int PREV_SLOT = 45;
    private static final int SEARCH_SLOT = 48;
    private static final int ALL_CATEGORIES_SLOT = 49;
    private static final int CLEAR_SEARCH_SLOT = 50;
    private static final int NEXT_SLOT = 53;

    private final DungeonManager dungeonManager;
    private final ChatPromptManager chatPromptManager;
    private final Plugin plugin;
    private final LangManager lang;

    private List<DungeonDefinition> filtered;
    private List<String> categories;
    private String activeCategory;
    private String searchText = "";

    public DungeonBrowserGUI(Player player, DungeonManager dungeonManager, ChatPromptManager chatPromptManager,
            Plugin plugin) {

        super(player, ComponentUtils.parse(chatPromptManager.lang().raw("gui.browser.title")), SIZE, CONTENT_SLOTS);

        this.dungeonManager = dungeonManager;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;
        this.lang = chatPromptManager.lang();

        recomputeCategories();
        applyFilters();
    }

    private void recomputeCategories() {

        Set<String> unique = new LinkedHashSet<>();
        for (DungeonDefinition definition : dungeonManager.getAll()) {
            unique.add(definition.category());
        }

        categories = new ArrayList<>(unique);
    }

    private void applyFilters() {

        String search = searchText.toLowerCase(Locale.ROOT);

        filtered = dungeonManager.getAll().stream()
                .filter(def -> activeCategory == null || def.category().equalsIgnoreCase(activeCategory))
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

        DungeonDefinition definition = filtered.get(absoluteIndex);
        Material icon = resolveIcon(definition);

        setItem(contentSlot, new ItemBuilder(icon)
                .setName(ComponentUtils.parse(definition.displayName())
                        .colorIfAbsent(NamedTextColor.WHITE))
                .setLore(
                        Component.text(definition.id(), NamedTextColor.DARK_GRAY),
                        ComponentUtils.parse(lang.raw("gui.browser.item.level_players",
                                "level", definition.recommendedLevel(), "min", definition.minPlayers(),
                                "max", definition.maxPlayers())),
                        ComponentUtils.parse(lang.raw("gui.browser.item.rooms", "rooms", definition.rooms().size())),
                        ComponentUtils.parse(lang.raw("gui.browser.item.click_to_edit")))
                .build());
    }

    private Material resolveIcon(DungeonDefinition definition) {
        try {
            return Material.valueOf(definition.icon());
        } catch (IllegalArgumentException e) {
            return Material.STONE_BRICKS;
        }
    }

    @Override
    protected void renderExtras() {

        for (int i = 0; i < categories.size() && i < MAX_CATEGORY_BUTTONS; i++) {

            String category = categories.get(i);
            boolean active = category.equalsIgnoreCase(activeCategory);

            setItem(CATEGORY_ROW_START + i, new ItemBuilder(active ? Material.CHEST : Material.BARREL)
                    .setName(Component.text(category, active ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                    .build());
        }

        setItem(PREV_SLOT, hasPreviousPage()
                ? new ItemBuilder(Material.ARROW).setName(ComponentUtils.parse(lang.raw("gui.common.previous"))).build()
                : ItemBuilder.createFiller());

        setItem(NEXT_SLOT, hasNextPage()
                ? new ItemBuilder(Material.ARROW).setName(ComponentUtils.parse(lang.raw("gui.common.next"))).build()
                : ItemBuilder.createFiller());

        setItem(SEARCH_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(ComponentUtils.parse(lang.raw("gui.browser.search.name")))
                .setLore(ComponentUtils.parse(searchText.isBlank() ? lang.raw("gui.browser.search.no_filter")
                        : lang.raw("gui.browser.search.filter", "text", searchText)))
                .build());

        setItem(ALL_CATEGORIES_SLOT, new ItemBuilder(activeCategory == null ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(ComponentUtils.parse(lang.raw("gui.browser.all_categories")))
                .build());

        setItem(CLEAR_SEARCH_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parse(lang.raw("gui.browser.clear_search")))
                .build());
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {

        DungeonDefinition definition = filtered.get(absoluteIndex);
        DungeonEditorSession session = new DungeonEditorSession(definition, dungeonManager, chatPromptManager,
                plugin);

        new DungeonEditorHubGUI(player, session).open();
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
        chatPromptManager.prompt(player, "gui.browser.search.prompt", value -> {

            searchText = value.trim();
            applyFilters();
        });
    }

}
