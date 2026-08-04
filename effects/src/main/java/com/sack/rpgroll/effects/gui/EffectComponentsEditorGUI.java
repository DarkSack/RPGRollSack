package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.effects.core.EffectComponent;
import com.sack.rpgroll.effects.core.EffectComponentType;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.core.EffectTriggerType;
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

/** Qué hace el efecto mientras está activo — cada componente dispara en su propio {@link EffectTriggerType}. */
public class EffectComponentsEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int LIST_SLOTS = 45;
    private static final int ADD_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final EffectManager effectManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private EffectDefinition current;

    public EffectComponentsEditorGUI(Player player, EffectDefinition effect, EffectManager effectManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Componentes: " + effect.id(), NamedTextColor.GOLD), SIZE);
        this.current = effect;
        this.effectManager = effectManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(List<EffectComponent> components) {
        current = new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), current.durationTicks(),
                current.priority(), current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), components);
        effectManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<EffectComponent> components = current.components();

        for (int i = 0; i < components.size() && i < LIST_SLOTS; i++) {

            EffectComponent component = components.get(i);

            setItem(i, new ItemBuilder(iconFor(component.type()))
                    .setName(Component.text("#" + (i + 1) + " " + component.type(), NamedTextColor.AQUA))
                    .setLore(Component.text("Trigger: " + component.trigger(), NamedTextColor.GRAY),
                            Component.text(component.params().toString(), NamedTextColor.DARK_GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.RED))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar componente", NamedTextColor.GREEN))
                .setLore(Component.text("TIPO TRIGGER clave=valor,clave2=valor2", NamedTextColor.GRAY),
                        Component.text("ej. PERIODIC_DAMAGE ON_SECOND amount=2,amount-per-stack=1",
                                NamedTextColor.DARK_GRAY),
                        Component.text("ej. VISUAL ON_APPLY effect=level_up", NamedTextColor.DARK_GRAY),
                        Component.text("tipos: " + typeList(), NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private Material iconFor(EffectComponentType type) {
        return switch (type) {
            case ATTRIBUTE_MODIFIER, MOVEMENT_MODIFIER -> Material.IRON_INGOT;
            case PERIODIC_DAMAGE -> Material.REDSTONE;
            case PERIODIC_HEAL -> Material.GLISTERING_MELON_SLICE;
            case POTION_VANILLA -> Material.POTION;
            case VISUAL -> Material.BLAZE_POWDER;
            case SOUND -> Material.NOTE_BLOCK;
            case MESSAGE -> Material.PAPER;
            case SHIELD -> Material.SHIELD;
            case SILENCE -> Material.BARRIER;
            case CONFUSION -> Material.ENDER_EYE;
            case AURA -> Material.BEACON;
            case COMMAND -> Material.COMMAND_BLOCK;
        };
    }

    private String typeList() {
        StringBuilder builder = new StringBuilder();
        for (EffectComponentType type : EffectComponentType.values()) {
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

        if (slot < current.components().size() && slot < LIST_SLOTS) {
            if (event.isShiftClick()) {
                List<EffectComponent> updated = new ArrayList<>(current.components());
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
        chatPromptManager.prompt(player, "Escribí: TIPO TRIGGER clave=valor,clave2=valor2", value -> {

            String[] parts = value.trim().split("\\s+", 3);

            if (parts.length < 2) {
                player.sendMessage(Component.text("Formato inválido — hacen falta al menos TIPO y TRIGGER.",
                        NamedTextColor.RED));
                return;
            }

            EffectComponentType type;

            try {
                type = EffectComponentType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Tipo inválido: " + parts[0], NamedTextColor.RED));
                return;
            }

            EffectTriggerType trigger;

            try {
                trigger = EffectTriggerType.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Trigger inválido: " + parts[1], NamedTextColor.RED));
                return;
            }

            Map<String, String> params = new LinkedHashMap<>();

            if (parts.length == 3) {
                for (String pair : parts[2].split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            List<EffectComponent> updated = new ArrayList<>(current.components());
            updated.add(new EffectComponent(type, trigger, params));
            replace(updated);
        });
    }

}
