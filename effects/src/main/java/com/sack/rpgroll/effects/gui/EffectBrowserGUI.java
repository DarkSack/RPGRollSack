package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.effects.core.EffectCategory;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.runtime.EffectTracker;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/**
 * Navegador del Effect Studio — lista, busca (por substring de id/nombre),
 * filtra por categoría (ciclando con click) y permite duplicar un efecto
 * existente (shift-click) o crear uno nuevo. Versionado/importar-exportar
 * quedan fuera de esta pasada.
 */
public class EffectBrowserGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int LIST_SLOTS = 45;

    private static final int SEARCH_SLOT = 45;
    private static final int FILTER_SLOT = 46;
    private static final int NEW_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final EffectManager effectManager;
    private final EffectTracker tracker;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    private String searchQuery = "";
    private EffectCategory filterCategory;
    private List<EffectDefinition> filtered;

    public EffectBrowserGUI(Player player, EffectManager effectManager, EffectTracker tracker,
            ChatPromptManager chatPromptManager) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.browser.title"), NamedTextColor.GOLD), SIZE);
        this.effectManager = effectManager;
        this.tracker = tracker;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        recomputeFiltered();
    }

    private void recomputeFiltered() {
        filtered = effectManager.getAll().stream()
                .filter(effect -> filterCategory == null || effect.category() == filterCategory)
                .filter(effect -> searchQuery.isBlank()
                        || effect.id().toLowerCase(Locale.ROOT).contains(searchQuery)
                        || effect.displayName().toLowerCase(Locale.ROOT).contains(searchQuery))
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < filtered.size() && i < LIST_SLOTS; i++) {

            EffectDefinition effect = filtered.get(i);
            Material icon = parseMaterial(effect.icon());

            setItem(i, new ItemBuilder(icon)
                    .setName(Component.text(effect.displayName(), effect.rarity().color()))
                    .setLore(lang.component("gui.common.id_label", "id", effect.id()),
                            lang.component("gui.browser.item_category_components", "category", effect.category(),
                                    "count", effect.components().size()),
                            lang.component("gui.common.click_to_edit"),
                            lang.component("gui.browser.shift_duplicate"))
                    .build());
        }

        setItem(SEARCH_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(lang.component("gui.browser.search_label", "text",
                        searchQuery.isBlank() ? lang.raw("gui.browser.search_none") : searchQuery))
                .setLore(lang.component("gui.browser.search_hint"),
                        lang.component("gui.browser.clear_hint"))
                .build());

        setItem(FILTER_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(lang.component("gui.browser.filter_label", "text",
                        filterCategory == null ? lang.raw("gui.browser.filter_all") : filterCategory))
                .setLore(lang.component("gui.common.click_cycle"))
                .build());

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.browser.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close_button")));
    }

    private Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.NETHER_STAR;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < filtered.size() && slot < LIST_SLOTS) {

            EffectDefinition effect = filtered.get(slot);

            if (event.isShiftClick()) {
                promptDuplicate(effect);
            } else {
                new EffectEditorHubGUI(player, effect, effectManager, tracker, chatPromptManager, this::reopen)
                        .open();
            }

            return;
        }

        if (slot == SEARCH_SLOT) {
            if (event.isRightClick()) {
                searchQuery = "";
                reopen();
            } else {
                promptSearch();
            }
            return;
        }

        if (slot == FILTER_SLOT) {
            cycleFilter();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void cycleFilter() {

        EffectCategory[] categories = EffectCategory.values();

        if (filterCategory == null) {
            filterCategory = categories[0];
        } else {
            int next = filterCategory.ordinal() + 1;
            filterCategory = next >= categories.length ? null : categories[next];
        }

        reopen();
    }

    private void promptSearch() {
        chatPromptManager.prompt(player, "gui.browser.prompt_search", value -> {
            searchQuery = value.trim().toLowerCase(Locale.ROOT);
            reopen();
        });
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "gui.browser.prompt_new", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (effectManager.exists(id)) {
                lang.send(player, "gui.common.id_exists");
                reopen();
                return;
            }

            EffectDefinition effect = new EffectDefinition(id, id, "NETHER_STAR", "WHITE",
                    EffectCategory.OTHER, com.sack.rpgroll.effects.core.EffectRarity.COMMON, "", 100, 0, true,
                    List.of(), java.util.Set.of(), java.util.Set.of(),
                    com.sack.rpgroll.effects.core.EffectStackingMode.NONE, 1, null, List.of());

            effectManager.save(effect);
            reopen();
        });
    }

    private void promptDuplicate(EffectDefinition original) {
        chatPromptManager.prompt(player, "gui.browser.prompt_duplicate", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (effectManager.exists(id)) {
                lang.send(player, "gui.common.id_exists");
                reopen();
                return;
            }

            EffectDefinition copy = new EffectDefinition(id, original.displayName() + " (copia)", original.icon(),
                    original.color(), original.category(), original.rarity(), original.description(),
                    original.durationTicks(), original.priority(), original.visible(), original.conditions(),
                    original.tags(), original.conflicts(), original.stackingMode(), original.maxStacks(),
                    original.upgradeToEffectId(), original.components());

            effectManager.save(copy);
            lang.send(player, "gui.browser.duplicate_success", "id", id);
            reopen();
        }, "id", original.id());
    }

    private void reopen() {
        recomputeFiltered();
        open();
    }

}
