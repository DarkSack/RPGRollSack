package com.sack.rpgroll.items.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Navegador visual de {@link Material} — cada slot muestra el material
 * real como su propio ícono. Permite filtrar escribiendo en el chat en
 * vez de tener que saber el nombre exacto de memoria.
 */
public class MaterialPickerGUI extends PaginatedGUI {

    private static final int SIZE = 54;
    private static final int CONTENT_SLOTS = 45;
    private static final int PREV_SLOT = 45;
    private static final int SEARCH_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int BACK_SLOT = 48;
    private static final int CLEAR_FILTER_SLOT = 50;

    private final ChatPromptManager chatPromptManager;
    private final Consumer<Material> onSelect;
    private final Runnable onBack;

    private List<Material> filtered;
    private String filterText = "";

    public MaterialPickerGUI(Player player, ChatPromptManager chatPromptManager, Consumer<Material> onSelect,
            Runnable onBack) {

        super(player, chatPromptManager.lang().component("gui.material_picker.title"), SIZE, CONTENT_SLOTS);

        this.chatPromptManager = chatPromptManager;
        this.onSelect = onSelect;
        this.onBack = onBack;
        this.filtered = allItemMaterials();
    }

    private List<Material> allItemMaterials() {

        List<Material> materials = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.isItem() && !material.isLegacy()) {
                materials.add(material);
            }
        }

        return materials;
    }

    @Override
    protected int totalItemCount() {
        return filtered.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        Material material = filtered.get(absoluteIndex);

        ItemStack preview = new ItemStack(material);
        var meta = preview.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(material.name(), NamedTextColor.YELLOW));
            preview.setItemMeta(meta);
        }

        setItem(contentSlot, preview);
    }

    @Override
    protected void renderExtras() {

        setItem(PREV_SLOT, hasPreviousPage()
                ? new ItemBuilder(Material.ARROW).setName(chatPromptManager.lang().component("gui.material_picker.prev")).build()
                : ItemBuilder.createFiller());

        setItem(NEXT_SLOT, hasNextPage()
                ? new ItemBuilder(Material.ARROW).setName(chatPromptManager.lang().component("gui.material_picker.next")).build()
                : ItemBuilder.createFiller());

        setItem(SEARCH_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(chatPromptManager.lang().component("gui.material_picker.search"))
                .setLore(filterText.isBlank()
                        ? chatPromptManager.lang().component("gui.material_picker.no_filter")
                        : chatPromptManager.lang().component("gui.material_picker.filter", "text", filterText))
                .build());

        setItem(CLEAR_FILTER_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(chatPromptManager.lang().component("gui.material_picker.clear_filter"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("editor.common.back")));
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
        onSelect.accept(filtered.get(absoluteIndex));
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        switch (event.getSlot()) {
            case PREV_SLOT -> previousPage();
            case NEXT_SLOT -> nextPage();
            case SEARCH_SLOT -> promptSearch();
            case CLEAR_FILTER_SLOT -> {
                filterText = "";
                applyFilter();
            }
            case BACK_SLOT -> onBack.run();
            default -> {
            }
        }
    }

    private void promptSearch() {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.material_picker.prompt_search"), value -> {
            filterText = value.trim().toLowerCase();
            applyFilter();
        });
    }

    private void applyFilter() {

        List<Material> all = allItemMaterials();

        filtered = filterText.isBlank()
                ? all
                : all.stream().filter(m -> m.name().toLowerCase().contains(filterText)).toList();

        page = 0;
        build();
    }

}
