package com.sack.rpgroll.items.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.gui.editor.EditorSession;
import com.sack.rpgroll.items.gui.editor.ItemEditorHubGUI;
import com.sack.rpgroll.items.rarity.RarityManager;
import com.sack.rpgroll.items.registry.StatRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Navegador de ítems: grid paginado con la apariencia real de cada ítem
 * como ícono, filtro por categoría, y búsqueda por nombre/id. Click
 * izquierdo entrega el ítem, click derecho abre el editor completo.
 */
public class ItemBrowserGUI extends PaginatedGUI {

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

    private final ItemManager itemManager;
    private final ItemFactory itemFactory;
    private final RarityManager rarityManager;
    private final StatRegistry statRegistry;
    private final ChatPromptManager chatPromptManager;
    private final Plugin plugin;

    private List<ItemDefinition> filtered;
    private List<String> categories;
    private String activeCategory = null;
    private String searchText = "";

    public ItemBrowserGUI(Player player, ItemManager itemManager, ItemFactory itemFactory,
            RarityManager rarityManager, StatRegistry statRegistry, ChatPromptManager chatPromptManager,
            Plugin plugin) {

        super(player, Component.text("Ítems RPGRoll", NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);

        this.itemManager = itemManager;
        this.itemFactory = itemFactory;
        this.rarityManager = rarityManager;
        this.statRegistry = statRegistry;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;

        recomputeCategories();
        applyFilters();
    }

    private void recomputeCategories() {

        Set<String> unique = new LinkedHashSet<>();
        for (ItemDefinition definition : itemManager.getAll()) {
            unique.add(definition.category());
        }

        categories = new ArrayList<>(unique);
    }

    private void applyFilters() {

        List<ItemDefinition> all = new ArrayList<>(itemManager.getAll());

        filtered = all.stream()
                .filter(def -> activeCategory == null || def.category().equalsIgnoreCase(activeCategory))
                .filter(def -> searchText.isBlank()
                        || def.id().toLowerCase().contains(searchText)
                        || def.displayName().toLowerCase().contains(searchText))
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
        setItem(contentSlot, itemFactory.create(filtered.get(absoluteIndex)));
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
                ? new ItemBuilder(Material.ARROW).setName(Component.text("« Anterior", NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(NEXT_SLOT, hasNextPage()
                ? new ItemBuilder(Material.ARROW).setName(Component.text("Siguiente »", NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(SEARCH_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(Component.text("Buscar", NamedTextColor.AQUA))
                .setLore(Component.text(searchText.isBlank() ? "(sin filtro)" : "Filtro: " + searchText,
                        NamedTextColor.GRAY))
                .build());

        setItem(ALL_CATEGORIES_SLOT, new ItemBuilder(activeCategory == null ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(Component.text("Todas las categorías", NamedTextColor.AQUA))
                .build());

        setItem(CLEAR_SEARCH_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Limpiar búsqueda", NamedTextColor.RED))
                .build());

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear ítem nuevo", NamedTextColor.GREEN))
                .build());
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {

        ItemDefinition definition = filtered.get(absoluteIndex);

        if (event.getClick() == ClickType.RIGHT) {

            EditorSession session = new EditorSession(definition, itemFactory, itemManager, rarityManager,
                    statRegistry, chatPromptManager, plugin);

            new ItemEditorHubGUI(player, session).open();
            return;
        }

        ItemStack item = itemFactory.create(definition);
        var leftover = player.getInventory().addItem(item);
        leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));

        player.sendMessage(Component.text("✔ Recibiste: " + definition.displayName(), NamedTextColor.GREEN));
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        int slot = event.getSlot();

        if (slot >= CATEGORY_ROW_START && slot < CATEGORY_ROW_START + Math.min(categories.size(), MAX_CATEGORY_BUTTONS)) {
            activeCategory = categories.get(slot - CATEGORY_ROW_START);
            applyFilters();
            return;
        }

        switch (slot) {
            case PREV_SLOT -> previousPage();
            case NEXT_SLOT -> nextPage();
            case SEARCH_SLOT -> promptSearch();
            case NEW_SLOT -> promptNewItem();
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
        chatPromptManager.prompt(player, "Escribí parte del nombre o id del ítem:", value -> {
            searchText = value.trim().toLowerCase();
            applyFilters();
        });
    }

    private void promptNewItem() {
        chatPromptManager.prompt(player, "Escribí: id;categoria (ej. flame_blade;sword):", value -> {

            String[] parts = value.split(";", 2);
            String id = parts[0].trim().toLowerCase().replace(' ', '_');
            String category = parts.length > 1 ? parts[1].trim().toLowerCase() : "misc";

            if (id.isBlank() || itemManager.exists(id)) {
                player.sendMessage(Component.text("Id inválido o ya existente.", NamedTextColor.RED));
                return;
            }

            ItemDefinition definition = new ItemDefinition(id, category, Material.PAPER, id, null, null, null, null,
                    null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, 0, 0, null);

            EditorSession session = new EditorSession(definition, itemFactory, itemManager, rarityManager,
                    statRegistry, chatPromptManager, plugin);

            new ItemEditorHubGUI(player, session).open();
        });
    }

}
