package com.sack.rpgroll.sackeffects.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.sackeffects.core.EffectDefinition;
import com.sack.rpgroll.sackeffects.core.EffectManager;
import com.sack.rpgroll.sackeffects.core.EffectStep;
import com.sack.rpgroll.sackeffects.core.EffectStepType;
import com.sack.rpgroll.sackeffects.engine.EffectContext;
import com.sack.rpgroll.sackeffects.engine.EffectEngine;
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

/**
 * Editor de un {@link EffectDefinition}: nombre, descripción, y su lista de
 * steps (agregar/quitar por chat con la misma sintaxis compacta que el
 * resto del proyecto: {@code TIPO delay clave=valor,clave2=valor2}). El
 * botón "Probar" dispara el efecto completo sobre vos mismo ahí mismo, sin
 * salir de la GUI — al ser algo visual/sonoro, probarlo en el momento vale
 * mucho más que leer los parámetros.
 */
public class EffectEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int DISPLAY_NAME_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 11;
    private static final int TEST_SLOT = 13;

    private static final int STEPS_START_SLOT = 18;
    private static final int STEPS_MAX = 18;
    private static final int ADD_STEP_SLOT = 40;
    private static final int BACK_SLOT = 49;

    private final EffectManager manager;
    private final EffectEngine engine;
    private final ChatPromptManager chatPromptManager;
    private final LangManager langManager;
    private final Runnable onBack;
    private EffectDefinition current;

    public EffectEditorGUI(Player player, EffectDefinition effect, EffectManager manager, EffectEngine engine,
            ChatPromptManager chatPromptManager, LangManager langManager, Runnable onBack) {
        super(player, langManager.component("editor.title", "id", effect.id()), SIZE);
        this.current = effect;
        this.manager = manager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
        this.langManager = langManager;
        this.onBack = onBack;
    }

    private void replace(EffectDefinition updated) {
        current = updated;
        manager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        setItem(DISPLAY_NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(langManager.component("editor.name_item", "name", current.displayName()))
                .setLore(langManager.component("editor.name_lore_hint"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(langManager.component("editor.description_item"))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? langManager.raw("editor.description_empty")
                        : current.description()))
                .build());

        setItem(TEST_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(langManager.component("editor.test_item"))
                .setLore(langManager.component("editor.item_lore_steps", "count", current.steps().size()))
                .build());

        List<EffectStep> steps = current.steps();

        for (int i = 0; i < steps.size() && i < STEPS_MAX; i++) {
            setItem(STEPS_START_SLOT + i, stepItem(steps.get(i), i));
        }

        setItem(ADD_STEP_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(langManager.component("editor.add_step_item"))
                .setLore(langManager.component("editor.add_step_lore_syntax"),
                        langManager.component("editor.add_step_lore_example_particle"),
                        langManager.component("editor.add_step_lore_example_sound"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(langManager.raw("editor.back_button")));
    }

    private org.bukkit.inventory.ItemStack stepItem(EffectStep step, int index) {

        List<Component> lore = new ArrayList<>();
        lore.add(langManager.component("editor.step_delay_lore", "ticks", step.delayTicks()));

        for (var entry : step.params().entrySet()) {
            lore.add(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.DARK_GRAY));
        }

        lore.add(langManager.component("editor.step_remove_hint"));

        return new ItemBuilder(iconFor(step.type()))
                .setName(Component.text("#" + (index + 1) + " " + step.type(), NamedTextColor.AQUA))
                .setLore(lore)
                .build();
    }

    private Material iconFor(EffectStepType type) {
        return switch (type) {
            case PARTICLE -> Material.BLAZE_POWDER;
            case SOUND -> Material.NOTE_BLOCK;
            case TITLE -> Material.OAK_SIGN;
            case ACTIONBAR -> Material.PAPER;
            case BOSSBAR -> Material.DRAGON_HEAD;
            case POTION -> Material.POTION;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == DISPLAY_NAME_SLOT) {
            promptRename();
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            promptDescription();
            return;
        }

        if (slot == TEST_SLOT) {
            engine.play(current, EffectContext.of(player));
            langManager.send(player, "editor.test_playing", "id", current.id());
            return;
        }

        int stepIndex = slot - STEPS_START_SLOT;
        if (stepIndex >= 0 && stepIndex < current.steps().size() && stepIndex < STEPS_MAX) {
            if (event.isShiftClick()) {
                removeStep(stepIndex);
            }
            return;
        }

        if (slot == ADD_STEP_SLOT) {
            promptAddStep();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptRename() {
        chatPromptManager.prompt(player, langManager.raw("editor.rename_prompt"),
                value -> replace(new EffectDefinition(current.id(), value, current.description(), current.steps())));
    }

    private void promptDescription() {
        chatPromptManager.prompt(player, langManager.raw("editor.description_prompt"),
                value -> replace(new EffectDefinition(current.id(), current.displayName(), value, current.steps())));
    }

    private void removeStep(int index) {
        List<EffectStep> steps = new ArrayList<>(current.steps());
        steps.remove(index);
        replace(new EffectDefinition(current.id(), current.displayName(), current.description(), steps));
    }

    private void promptAddStep() {
        chatPromptManager.prompt(player,
                langManager.raw("editor.add_step_prompt"),
                value -> {

                    String[] parts = value.trim().split("\\s+", 3);

                    if (parts.length < 2) {
                        langManager.send(player, "editor.add_step_invalid_format");
                        return;
                    }

                    EffectStepType type;

                    try {
                        type = EffectStepType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        langManager.send(player, "editor.add_step_invalid_type", "type", parts[0]);
                        return;
                    }

                    int delay;

                    try {
                        delay = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        langManager.send(player, "editor.add_step_invalid_delay", "delay", parts[1]);
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

                    List<EffectStep> steps = new ArrayList<>(current.steps());
                    steps.add(new EffectStep(type, delay, params));

                    replace(new EffectDefinition(current.id(), current.displayName(), current.description(), steps));
                });
    }

}
