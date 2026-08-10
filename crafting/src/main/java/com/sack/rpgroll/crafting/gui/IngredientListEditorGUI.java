package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Editor reusable de {@code List<IngredientSpec>} — mismo patrón que {@link RecipeConditionsEditorGUI}. */
public class IngredientListEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int LIST_SLOTS = 36;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ChatPromptManager chatPromptManager;
    private final Consumer<List<IngredientSpec>> onSave;
    private final Runnable onBack;
    private List<IngredientSpec> ingredients;

    public IngredientListEditorGUI(Player player, String title, List<IngredientSpec> initial,
            ChatPromptManager chatPromptManager, Consumer<List<IngredientSpec>> onSave, Runnable onBack) {
        super(player, Component.text(title, NamedTextColor.GOLD), SIZE);
        this.ingredients = new ArrayList<>(initial);
        this.chatPromptManager = chatPromptManager;
        this.onSave = onSave;
        this.onBack = onBack;
    }

    private void replace(List<IngredientSpec> updated) {
        this.ingredients = updated;
        onSave.accept(List.copyOf(updated));
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < ingredients.size() && i < LIST_SLOTS; i++) {

            IngredientSpec spec = ingredients.get(i);

            setItem(i, new ItemBuilder(Material.HOPPER)
                    .setName(Component.text(IngredientSpecFormat.format(spec), NamedTextColor.AQUA))
                    .setLore(Component.text("Shift-click para quitar", NamedTextColor.RED))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar ingrediente", NamedTextColor.GREEN))
                .setLore(Component.text("TIPO VALOR CANTIDAD [CALIDAD-MINIMA]", NamedTextColor.GRAY),
                        Component.text("ej. MATERIAL IRON_INGOT 2", NamedTextColor.DARK_GRAY),
                        Component.text("ej. ITEM_ID enchanted_gem 1 FINE", NamedTextColor.DARK_GRAY),
                        Component.text("ej. TAG minecraft:planks 4", NamedTextColor.DARK_GRAY),
                        Component.text("ej. ANY 1", NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < ingredients.size() && slot < LIST_SLOTS) {
            if (event.isShiftClick()) {
                List<IngredientSpec> updated = new ArrayList<>(ingredients);
                updated.remove(slot);
                replace(updated);
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAdd() {
        chatPromptManager.prompt(player, "Escribí: TIPO VALOR CANTIDAD [CALIDAD-MINIMA]", raw -> {

            IngredientSpec spec;
            try {
                spec = IngredientSpecFormat.parse(raw);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
                return;
            }

            List<IngredientSpec> updated = new ArrayList<>(ingredients);
            updated.add(spec);
            replace(updated);
        });
    }

}
