package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.core.EffectStackingMode;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Cómo se acumula el efecto al reaplicarse, con qué otros efectos entra en conflicto, y sus tags de inmunidad. */
public class EffectStackingEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int MODE_SLOT = 11;
    private static final int MAX_STACKS_SLOT = 12;
    private static final int UPGRADE_TO_SLOT = 13;
    private static final int TAGS_SLOT = 20;
    private static final int CONFLICTS_SLOT = 21;
    private static final int BACK_SLOT = 44;

    private final EffectManager effectManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private EffectDefinition current;

    public EffectStackingEditorGUI(Player player, EffectDefinition effect, EffectManager effectManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.stacking.title", "id", effect.id()), NamedTextColor.GOLD), SIZE);
        this.current = effect;
        this.effectManager = effectManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(EffectStackingMode mode, int maxStacks, String upgradeTo, Set<String> tags,
            Set<String> conflicts) {
        current = new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), current.durationTicks(),
                current.priority(), current.visible(), current.conditions(), tags, conflicts, mode, maxStacks,
                upgradeTo, current.components());
        effectManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(MODE_SLOT, new ItemBuilder(Material.REPEATER)
                .setName(lang.component("gui.stacking.mode_label", "mode", current.stackingMode()))
                .setLore(lang.component("gui.stacking.mode_none"),
                        lang.component("gui.stacking.mode_independent"),
                        lang.component("gui.stacking.mode_refresh"),
                        lang.component("gui.stacking.mode_upgrade"),
                        lang.component("gui.common.click_cycle"))
                .build());

        setItem(MAX_STACKS_SLOT, new ItemBuilder(Material.SLIME_BALL)
                .setName(lang.component("gui.stacking.max_stacks_label", "value", current.maxStacks()))
                .setLore(lang.component("gui.stacking.max_stacks_note"),
                        lang.component("gui.editor.priority_hint"))
                .build());

        setItem(UPGRADE_TO_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.stacking.upgrade_label", "value",
                        current.upgradeToEffectId() == null ? lang.raw("gui.common.none_label")
                                : current.upgradeToEffectId()))
                .setLore(lang.component("gui.stacking.upgrade_note"),
                        lang.component("gui.stacking.upgrade_hint"))
                .build());

        setItem(TAGS_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.stacking.tags_label", "value", String.join(", ", current.tags())))
                .setLore(lang.component("gui.stacking.tags_note"),
                        lang.component("gui.common.click_write_list"))
                .build());

        setItem(CONFLICTS_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(lang.component("gui.stacking.conflicts_label", "value",
                        String.join(", ", current.conflicts())))
                .setLore(lang.component("gui.stacking.conflicts_note"),
                        lang.component("gui.common.click_write_list"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == MODE_SLOT) {
            EffectStackingMode[] values = EffectStackingMode.values();
            EffectStackingMode next = values[(current.stackingMode().ordinal() + 1) % values.length];
            replace(next, current.maxStacks(), current.upgradeToEffectId(), current.tags(), current.conflicts());
            return;
        }

        if (slot == MAX_STACKS_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            int newMax = Math.max(1, current.maxStacks() + delta);
            replace(current.stackingMode(), newMax, current.upgradeToEffectId(), current.tags(),
                    current.conflicts());
            return;
        }

        if (slot == UPGRADE_TO_SLOT) {
            promptUpgradeTo();
            return;
        }

        if (slot == TAGS_SLOT) {
            promptTags();
            return;
        }

        if (slot == CONFLICTS_SLOT) {
            promptConflicts();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptUpgradeTo() {
        chatPromptManager.prompt(player, "gui.stacking.prompt_upgrade",
                value -> {
                    String upgradeTo = value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
                    replace(current.stackingMode(), current.maxStacks(), upgradeTo, current.tags(),
                            current.conflicts());
                });
    }

    private void promptTags() {
        chatPromptManager.prompt(player, "gui.stacking.prompt_tags", value -> {
            Set<String> tags = value.isBlank() ? Set.of()
                    : new LinkedHashSet<>(java.util.List.of(value.toLowerCase(Locale.ROOT).split("\\s*,\\s*")));
            replace(current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), tags,
                    current.conflicts());
        });
    }

    private void promptConflicts() {
        chatPromptManager.prompt(player, "gui.stacking.prompt_conflicts", value -> {
            Set<String> conflicts = value.isBlank() ? Set.of()
                    : new LinkedHashSet<>(java.util.List.of(value.toLowerCase(Locale.ROOT).split("\\s*,\\s*")));
            replace(current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.tags(),
                    conflicts);
        });
    }

}
