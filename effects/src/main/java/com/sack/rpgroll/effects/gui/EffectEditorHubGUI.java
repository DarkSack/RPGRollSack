package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.effects.core.EffectCategory;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.core.EffectRarity;
import com.sack.rpgroll.effects.runtime.EffectTracker;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

/**
 * Pantalla principal de un efecto — identidad (nombre/ícono/color/categoría
 * /rareza/descripción/duración/prioridad/visible) más accesos a las 3
 * subpantallas (condiciones, componentes, stacking/tags/conflictos) y un
 * botón "Probar" que lo aplica sobre vos mismo ahí mismo, sin salir de la
 * GUI ni pasar por el chequeo de condiciones (es una prueba, no un uso real).
 */
public class EffectEditorHubGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int ICON_SLOT = 0;
    private static final int COLOR_SLOT = 1;
    private static final int CATEGORY_SLOT = 2;
    private static final int RARITY_SLOT = 3;
    private static final int NAME_SLOT = 4;
    private static final int DESCRIPTION_SLOT = 5;
    private static final int DURATION_SLOT = 6;
    private static final int PRIORITY_SLOT = 7;
    private static final int VISIBLE_SLOT = 8;

    private static final int CONDITIONS_SLOT = 19;
    private static final int COMPONENTS_SLOT = 20;
    private static final int STACKING_SLOT = 21;

    private static final int TEST_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final EffectManager effectManager;
    private final EffectTracker tracker;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private EffectDefinition current;

    public EffectEditorHubGUI(Player player, EffectDefinition effect, EffectManager effectManager,
            EffectTracker tracker, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.editor.title", "id", effect.id()),
                NamedTextColor.GOLD), SIZE);
        this.current = effect;
        this.effectManager = effectManager;
        this.tracker = tracker;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(EffectDefinition updated) {
        current = updated;
        effectManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 9; slot < 18; slot++) {
            setItem(slot, sectionGlass());
        }

        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        setItem(ICON_SLOT, new ItemBuilder(parseMaterial(current.icon()))
                .setName(lang.component("gui.editor.icon_label", "icon", current.icon()))
                .setLore(lang.component("gui.editor.icon_hint"))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.LIME_DYE)
                .setName(lang.component("gui.editor.color_label", "color", current.color()))
                .setLore(lang.component("gui.editor.color_hint"))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(lang.component("gui.editor.category_label", "category", current.category()))
                .setLore(lang.component("gui.common.click_cycle"))
                .build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text(lang.raw("gui.editor.rarity_label", "rarity", current.rarity()),
                        current.rarity().color()))
                .setLore(lang.component("gui.common.click_cycle"))
                .build());

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.name_label", "name", current.displayName()))
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.common.click_new_value"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.editor.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.editor.description_empty")
                                : current.description()))
                .build());

        String durationValue = current.durationTicks() <= 0 ? lang.raw("gui.editor.duration_permanent")
                : lang.raw("gui.editor.duration_ticks", "ticks", current.durationTicks());

        setItem(DURATION_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("gui.editor.duration_label", "value", durationValue))
                .setLore(lang.component("gui.editor.duration_hint1"),
                        lang.component("gui.editor.duration_hint2"),
                        lang.component("gui.editor.duration_note"))
                .build());

        setItem(PRIORITY_SLOT, new ItemBuilder(Material.LADDER)
                .setName(lang.component("gui.editor.priority_label", "value", current.priority()))
                .setLore(lang.component("gui.editor.priority_hint"))
                .build());

        setItem(VISIBLE_SLOT, new ItemBuilder(current.visible() ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(Component.text(
                        lang.raw("gui.editor.visible_label", "value",
                                current.visible() ? lang.raw("gui.common.yes_label") : lang.raw("gui.common.no_label")),
                        current.visible() ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .setLore(lang.component("gui.common.click_toggle"))
                .build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("gui.editor.conditions_name"))
                .setLore(lang.component("gui.editor.conditions_count", "count", current.conditions().size()),
                        lang.component("gui.common.click_to_edit"))
                .build());

        setItem(COMPONENTS_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(lang.component("gui.editor.components_name"))
                .setLore(lang.component("gui.editor.components_count", "count", current.components().size()),
                        lang.component("gui.common.click_to_edit"))
                .build());

        setItem(STACKING_SLOT, new ItemBuilder(Material.CHEST)
                .setName(lang.component("gui.editor.stacking_name"))
                .setLore(lang.component("gui.editor.stacking_mode_label", "mode", current.stackingMode()),
                        lang.component("gui.editor.stacking_counts", "tags", current.tags().size(), "conflicts",
                                current.conflicts().size()),
                        lang.component("gui.common.click_to_edit"))
                .build());

        setItem(TEST_SLOT, new ItemBuilder(Material.FIREWORK_ROCKET)
                .setName(lang.component("gui.editor.test_button"))
                .setLore(lang.component("gui.editor.test_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    private org.bukkit.inventory.ItemStack sectionGlass() {
        return new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
                .setName(Component.text(" ", NamedTextColor.GRAY)).build();
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
        ClickType click = event.getClick();
        boolean shift = event.isShiftClick();

        switch (slot) {
            case ICON_SLOT -> promptIcon();
            case COLOR_SLOT -> promptColor();
            case CATEGORY_SLOT -> cycleCategory();
            case RARITY_SLOT -> cycleRarity();
            case NAME_SLOT -> promptName();
            case DESCRIPTION_SLOT -> promptDescription();
            case DURATION_SLOT -> adjustDuration(click, shift);
            case PRIORITY_SLOT -> adjustPriority(click);
            case VISIBLE_SLOT -> toggleVisible();
            case CONDITIONS_SLOT -> new EffectConditionsEditorGUI(player, current, effectManager, chatPromptManager,
                    this::reopenFromSub).open();
            case COMPONENTS_SLOT -> new EffectComponentsEditorGUI(player, current, effectManager, chatPromptManager,
                    this::reopenFromSub).open();
            case STACKING_SLOT -> new EffectStackingEditorGUI(player, current, effectManager, chatPromptManager,
                    this::reopenFromSub).open();
            case TEST_SLOT -> testFire();
            case BACK_SLOT -> onBack.run();
            default -> {
            }
        }
    }

    private void reopenFromSub() {
        var refreshed = effectManager.get(current.id());
        refreshed.ifPresent(value -> current = value);
        build();
    }

    private void testFire() {
        tracker.apply(player, current, player);
        lang.send(player, "gui.editor.test_message", "id", current.id());
    }

    private void promptIcon() {
        chatPromptManager.prompt(player, "gui.editor.prompt_icon", value -> {
            try {
                Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                lang.send(player, "gui.common.invalid_material", "value", value);
                build();
                return;
            }
            replace(new EffectDefinition(current.id(), current.displayName(), value.trim().toUpperCase(Locale.ROOT),
                    current.color(), current.category(), current.rarity(), current.description(),
                    current.durationTicks(), current.priority(), current.visible(), current.conditions(),
                    current.tags(), current.conflicts(), current.stackingMode(), current.maxStacks(),
                    current.upgradeToEffectId(), current.components()));
        });
    }

    private void promptColor() {
        chatPromptManager.prompt(player, "gui.editor.prompt_color",
                value -> replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), value,
                        current.category(), current.rarity(), current.description(), current.durationTicks(),
                        current.priority(), current.visible(), current.conditions(), current.tags(),
                        current.conflicts(), current.stackingMode(), current.maxStacks(),
                        current.upgradeToEffectId(), current.components())));
    }

    private void cycleCategory() {
        EffectCategory[] values = EffectCategory.values();
        EffectCategory next = values[(current.category().ordinal() + 1) % values.length];
        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(), next,
                current.rarity(), current.description(), current.durationTicks(), current.priority(),
                current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components()));
    }

    private void cycleRarity() {
        EffectRarity[] values = EffectRarity.values();
        EffectRarity next = values[(current.rarity().ordinal() + 1) % values.length];
        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), next, current.description(), current.durationTicks(), current.priority(),
                current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components()));
    }

    private void promptName() {
        chatPromptManager.prompt(player, "gui.common.prompt_new_name",
                value -> replace(new EffectDefinition(current.id(), value, current.icon(), current.color(),
                        current.category(), current.rarity(), current.description(), current.durationTicks(),
                        current.priority(), current.visible(), current.conditions(), current.tags(),
                        current.conflicts(), current.stackingMode(), current.maxStacks(),
                        current.upgradeToEffectId(), current.components())));
    }

    private void promptDescription() {
        chatPromptManager.prompt(player, "gui.editor.prompt_description",
                value -> replace(new EffectDefinition(current.id(), current.displayName(), current.icon(),
                        current.color(), current.category(), current.rarity(), value, current.durationTicks(),
                        current.priority(), current.visible(), current.conditions(), current.tags(),
                        current.conflicts(), current.stackingMode(), current.maxStacks(),
                        current.upgradeToEffectId(), current.components())));
    }

    private void adjustDuration(ClickType click, boolean shift) {

        int delta = shift ? 200 : 20;
        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            delta = -delta;
        }

        int newDuration = Math.max(0, current.durationTicks() + delta);

        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), newDuration, current.priority(),
                current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components()));
    }

    private void adjustPriority(ClickType click) {

        int delta = click == ClickType.RIGHT ? -1 : 1;

        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), current.durationTicks(),
                current.priority() + delta, current.visible(), current.conditions(), current.tags(),
                current.conflicts(), current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(),
                current.components()));
    }

    private void toggleVisible() {
        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), current.durationTicks(),
                current.priority(), !current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components()));
    }

}
