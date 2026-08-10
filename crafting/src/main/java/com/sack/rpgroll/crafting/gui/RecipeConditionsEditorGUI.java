package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.condition.ConditionType;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Editor reusable de {@code List<RecipeCondition>} — usado por recetas
 * personalizadas, vanilla, de yunque y de fermentación por igual. No conoce
 * a qué tipo de contenido pertenece la lista: al modificarla, delega el
 * guardado a {@code onSave} (que arma el record correspondiente y llama a
 * su manager), mismo patrón que {@code EffectConditionsEditorGUI}.
 */
public class RecipeConditionsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int LIST_SLOTS = 36;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ChatPromptManager chatPromptManager;
    private final Consumer<List<RecipeCondition>> onSave;
    private final Runnable onBack;
    private List<RecipeCondition> conditions;

    public RecipeConditionsEditorGUI(Player player, String title, List<RecipeCondition> initial,
            ChatPromptManager chatPromptManager, Consumer<List<RecipeCondition>> onSave, Runnable onBack) {
        super(player, Component.text(title, NamedTextColor.GOLD), SIZE);
        this.conditions = new ArrayList<>(initial);
        this.chatPromptManager = chatPromptManager;
        this.onSave = onSave;
        this.onBack = onBack;
    }

    private void replace(List<RecipeCondition> updated) {
        this.conditions = updated;
        onSave.accept(List.copyOf(updated));
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < conditions.size() && i < LIST_SLOTS; i++) {

            RecipeCondition condition = conditions.get(i);

            setItem(i, new ItemBuilder(Material.COMPARATOR)
                    .setName(Component.text(condition.type().name(), NamedTextColor.AQUA))
                    .setLore(Component.text("valor: " + (condition.value() == null ? "(ninguno)" : condition.value()),
                                    NamedTextColor.GRAY),
                            Component.text("min-value: " + condition.minValue(), NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.RED))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar condición", NamedTextColor.GREEN))
                .setLore(Component.text("TIPO [valor] [min-value]", NamedTextColor.GRAY),
                        Component.text("ej. LEVEL_MIN 5", NamedTextColor.DARK_GRAY),
                        Component.text("ej. JOB_MIN blacksmith 3", NamedTextColor.DARK_GRAY),
                        Component.text("ej. GUILD_MEMBER", NamedTextColor.DARK_GRAY),
                        Component.text("tipos: " + typeList(), NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private String typeList() {
        StringBuilder builder = new StringBuilder();
        for (ConditionType type : ConditionType.values()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(type.name());
        }
        return builder.toString();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < conditions.size() && slot < LIST_SLOTS) {
            if (event.isShiftClick()) {
                List<RecipeCondition> updated = new ArrayList<>(conditions);
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
        chatPromptManager.prompt(player, "Escribí: TIPO [valor] [min-value]", raw -> {

            String[] parts = raw.trim().split("\\s+");

            ConditionType type;
            try {
                type = ConditionType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Tipo inválido: " + parts[0], NamedTextColor.RED));
                return;
            }

            String value = null;
            int minValue = 0;

            if (parts.length >= 2) {
                if (isInteger(parts[1])) {
                    minValue = Integer.parseInt(parts[1]);
                } else {
                    value = parts[1];
                    if (parts.length >= 3 && isInteger(parts[2])) {
                        minValue = Integer.parseInt(parts[2]);
                    }
                }
            }

            List<RecipeCondition> updated = new ArrayList<>(conditions);
            updated.add(new RecipeCondition(type, value, minValue));
            replace(updated);
        });
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
