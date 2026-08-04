package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.effects.core.EffectCondition;
import com.sack.rpgroll.effects.core.EffectConditionType;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Requisitos para poder aplicar el efecto — se evalúan una sola vez, al aplicar. */
public class EffectConditionsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int LIST_SLOTS = 36;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final EffectManager effectManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private EffectDefinition current;

    public EffectConditionsEditorGUI(Player player, EffectDefinition effect, EffectManager effectManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Condiciones: " + effect.id(), NamedTextColor.GOLD), SIZE);
        this.current = effect;
        this.effectManager = effectManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(List<EffectCondition> conditions) {
        current = new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), current.durationTicks(),
                current.priority(), current.visible(), conditions, current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components());
        effectManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<EffectCondition> conditions = current.conditions();

        for (int i = 0; i < conditions.size() && i < LIST_SLOTS; i++) {

            EffectCondition condition = conditions.get(i);

            setItem(i, new ItemBuilder(Material.COMPARATOR)
                    .setName(Component.text(condition.type().name(), NamedTextColor.AQUA))
                    .setLore(Component.text(condition.params().toString(), NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.RED))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar condición", NamedTextColor.GREEN))
                .setLore(Component.text("TIPO clave=valor,clave2=valor2", NamedTextColor.GRAY),
                        Component.text("ej. LEVEL_MIN value=10", NamedTextColor.DARK_GRAY),
                        Component.text("ej. WORLD value=world_nether", NamedTextColor.DARK_GRAY),
                        Component.text("tipos: " + typeList(), NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private String typeList() {
        StringBuilder builder = new StringBuilder();
        for (EffectConditionType type : EffectConditionType.values()) {
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

        if (slot < current.conditions().size() && slot < LIST_SLOTS) {
            if (event.isShiftClick()) {
                List<EffectCondition> updated = new ArrayList<>(current.conditions());
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
        chatPromptManager.prompt(player, "Escribí: TIPO clave=valor,clave2=valor2", value -> {

            String[] parts = value.trim().split("\\s+", 2);

            EffectConditionType type;

            try {
                type = EffectConditionType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Tipo inválido: " + parts[0], NamedTextColor.RED));
                return;
            }

            Map<String, String> params = new LinkedHashMap<>();

            if (parts.length == 2) {
                for (String pair : parts[1].split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            List<EffectCondition> updated = new ArrayList<>(current.conditions());
            updated.add(new EffectCondition(type, params));
            replace(updated);
        });
    }

}
