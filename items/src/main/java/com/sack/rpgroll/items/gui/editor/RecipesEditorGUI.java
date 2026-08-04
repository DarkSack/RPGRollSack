package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemRecipeDef;
import com.sack.rpgroll.items.core.RecipeType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editor de recetas. SHAPELESS/FURNACE/STONECUTTER se agregan en una
 * línea; SHAPED pide la forma y el mapeo de letras en dos pasos (no entra
 * cómodo en una sola línea de chat).
 */
public class RecipesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SHAPELESS_SLOT = 38;
    private static final int ADD_SHAPED_SLOT = 39;
    private static final int ADD_FURNACE_SLOT = 40;
    private static final int ADD_STONECUTTER_SLOT = 41;
    private static final int BACK_SLOT = 44;

    private final EditorSession session;
    private final Runnable onBack;

    public RecipesEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, Component.text("Recetas: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 36; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        for (int i = 0; i < session.recipes.size() && i < 36; i++) {

            ItemRecipeDef recipe = session.recipes.get(i);

            setItem(i, new ItemBuilder(Material.CRAFTING_TABLE)
                    .setName(Component.text(recipe.type().name(), NamedTextColor.YELLOW))
                    .setLore(Component.text(recipeSummary(recipe), NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SHAPELESS_SLOT, new ItemBuilder(Material.CRAFTING_TABLE)
                .setName(Component.text("+ Shapeless", NamedTextColor.GREEN)).build());
        setItem(ADD_SHAPED_SLOT, new ItemBuilder(Material.CRAFTING_TABLE)
                .setName(Component.text("+ Shaped", NamedTextColor.GREEN)).build());
        setItem(ADD_FURNACE_SLOT, new ItemBuilder(Material.FURNACE)
                .setName(Component.text("+ Furnace", NamedTextColor.GREEN)).build());
        setItem(ADD_STONECUTTER_SLOT, new ItemBuilder(Material.STONECUTTER)
                .setName(Component.text("+ Stonecutter", NamedTextColor.GREEN)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private String recipeSummary(ItemRecipeDef recipe) {

        return switch (recipe.type()) {
            case SHAPED -> String.join("|", recipe.shape());
            case SHAPELESS -> String.join(",", recipe.ingredients());
            case FURNACE, STONECUTTER -> recipe.ingredients().isEmpty() ? "?" : recipe.ingredients().get(0);
            default -> recipe.sourceId() == null ? "(sin datos)" : recipe.sourceId();
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.recipes.size() && slot < 36) {
            if (event.isShiftClick()) {
                session.recipes.remove(slot);
                build();
            }
            return;
        }

        switch (slot) {
            case ADD_SHAPELESS_SLOT -> promptShapeless();
            case ADD_SHAPED_SLOT -> promptShaped();
            case ADD_FURNACE_SLOT -> promptSimple(RecipeType.FURNACE);
            case ADD_STONECUTTER_SLOT -> promptSimple(RecipeType.STONECUTTER);
            case BACK_SLOT -> onBack.run();
            default -> {
            }
        }
    }

    private void promptShapeless() {
        session.chatPromptManager.prompt(player, "Escribí los ingredientes separados por coma:", value -> {

            List<String> ingredients = List.of(value.split(",")).stream().map(String::trim)
                    .filter(s -> !s.isEmpty()).map(s -> s.toUpperCase(Locale.ROOT)).toList();

            session.recipes.add(new ItemRecipeDef(RecipeType.SHAPELESS, List.of(), Map.of(), ingredients, null, 200,
                    null));
            build();
        });
    }

    private void promptShaped() {
        session.chatPromptManager.prompt(player,
                "Escribí las 3 filas separadas por '|' (usa espacio para vacío, ej. ' I | I | S '):", shapeValue -> {

                    List<String> shape = new ArrayList<>(List.of(shapeValue.split("\\|")));

                    session.chatPromptManager.prompt(player,
                            "Escribí el mapeo de letras: letra=MATERIAL,letra2=MATERIAL2 (ej. I=IRON_INGOT,S=STICK):",
                            keyValue -> {

                                Map<String, String> key = new HashMap<>();

                                for (String pair : keyValue.split(",")) {

                                    String[] kv = pair.split("=", 2);

                                    if (kv.length == 2) {
                                        key.put(kv[0].trim(), kv[1].trim().toUpperCase(Locale.ROOT));
                                    }
                                }

                                session.recipes.add(new ItemRecipeDef(RecipeType.SHAPED, shape, key, List.of(), null,
                                        200, null));
                                build();
                            });
                });
    }

    private void promptSimple(RecipeType type) {
        session.chatPromptManager.prompt(player, "Escribí el material de entrada (ej. IRON_ORE):", value -> {

            String material = value.trim().toUpperCase(Locale.ROOT);

            session.recipes.add(new ItemRecipeDef(type, List.of(), Map.of(), List.of(material), null, 200, null));
            build();
        });
    }

}
