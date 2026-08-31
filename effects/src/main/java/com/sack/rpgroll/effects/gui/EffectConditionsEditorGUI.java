package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private final Runnable onBack;
    private EffectDefinition current;

    public EffectConditionsEditorGUI(Player player, EffectDefinition effect, EffectManager effectManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.conditions.title", "id", effect.id()), NamedTextColor.GOLD), SIZE);
        this.current = effect;
        this.effectManager = effectManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
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
                            lang.component("gui.common.shift_remove"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.conditions.add"))
                .setLore(lang.component("gui.conditions.add_hint1"),
                        lang.component("gui.conditions.add_hint2"),
                        lang.component("gui.conditions.add_hint3"),
                        lang.component("gui.conditions.add_hint4", "types", typeList()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
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
        chatPromptManager.prompt(player, "gui.conditions.prompt_add", value -> {

            String[] parts = value.trim().split("\\s+", 2);

            EffectConditionType type;

            try {
                type = EffectConditionType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                lang.send(player, "gui.common.invalid_type", "value", parts[0]);
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
