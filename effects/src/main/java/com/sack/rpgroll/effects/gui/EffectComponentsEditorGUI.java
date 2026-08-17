package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private final Runnable onBack;
    private EffectDefinition current;

    public EffectComponentsEditorGUI(Player player, EffectDefinition effect, EffectManager effectManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.components.title", "id", effect.id()),
                NamedTextColor.GOLD), SIZE);
        this.current = effect;
        this.effectManager = effectManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
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
                    .setName(lang.component("gui.components.entry_label", "index", i + 1, "type", component.type()))
                    .setLore(lang.component("gui.components.trigger_label", "trigger", component.trigger()),
                            Component.text(component.params().toString(), NamedTextColor.DARK_GRAY),
                            lang.component("gui.common.shift_remove"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.components.add"))
                .setLore(lang.component("gui.components.add_hint1"),
                        lang.component("gui.components.add_hint2"),
                        lang.component("gui.components.add_hint3"),
                        lang.component("gui.components.add_hint4", "types", typeList()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
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
        chatPromptManager.prompt(player, "gui.components.prompt_add", value -> {

            String[] parts = value.trim().split("\\s+", 3);

            if (parts.length < 2) {
                lang.send(player, "gui.components.invalid_format");
                return;
            }

            EffectComponentType type;

            try {
                type = EffectComponentType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                lang.send(player, "gui.common.invalid_type", "value", parts[0]);
                return;
            }

            EffectTriggerType trigger;

            try {
                trigger = EffectTriggerType.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                lang.send(player, "gui.components.invalid_trigger", "value", parts[1]);
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
