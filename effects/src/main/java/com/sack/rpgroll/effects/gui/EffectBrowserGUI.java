package com.sack.rpgroll.effects.gui;

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

    private String searchQuery = "";
    private EffectCategory filterCategory;
    private List<EffectDefinition> filtered;

    public EffectBrowserGUI(Player player, EffectManager effectManager, EffectTracker tracker,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Effect Studio", NamedTextColor.GOLD), SIZE);
        this.effectManager = effectManager;
        this.tracker = tracker;
        this.chatPromptManager = chatPromptManager;
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
                    .setLore(Component.text("id: " + effect.id(), NamedTextColor.GRAY),
                            Component.text(effect.category() + " · " + effect.components().size() + " componente(s)",
                                    NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW),
                            Component.text("Shift-click para duplicar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(SEARCH_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(Component.text("Buscar: " + (searchQuery.isBlank() ? "(ninguna)" : searchQuery),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click para escribir un texto de búsqueda", NamedTextColor.GRAY),
                        Component.text("Click derecho para limpiar", NamedTextColor.GRAY))
                .build());

        setItem(FILTER_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Categoría: " + (filterCategory == null ? "TODAS" : filterCategory),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click para ciclar", NamedTextColor.GRAY))
                .build());

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear efecto nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
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
        chatPromptManager.prompt(player, "Escribí el texto a buscar:", value -> {
            searchQuery = value.trim().toLowerCase(Locale.ROOT);
            reopen();
        });
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id del nuevo efecto:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (effectManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un efecto con ese id.", NamedTextColor.RED));
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
        chatPromptManager.prompt(player, "Escribí el id para la copia de '" + original.id() + "':", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (effectManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un efecto con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            EffectDefinition copy = new EffectDefinition(id, original.displayName() + " (copia)", original.icon(),
                    original.color(), original.category(), original.rarity(), original.description(),
                    original.durationTicks(), original.priority(), original.visible(), original.conditions(),
                    original.tags(), original.conflicts(), original.stackingMode(), original.maxStacks(),
                    original.upgradeToEffectId(), original.components());

            effectManager.save(copy);
            player.sendMessage(Component.text("✔ Duplicado como '" + id + "'.", NamedTextColor.GREEN));
            reopen();
        });
    }

    private void reopen() {
        recomputeFiltered();
        open();
    }

}
