package com.sack.rpgroll.fx.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fx.core.EffectDefinition;
import com.sack.rpgroll.fx.core.EffectManager;
import com.sack.rpgroll.fx.engine.EffectContext;
import com.sack.rpgroll.fx.engine.EffectEngine;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EffectBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;
    private static final int DESCRIPTION_WIDTH = 38;

    private final EffectManager effectManager;
    private final EffectEngine engine;
    private final ChatPromptManager chatPromptManager;
    private final LangManager langManager;
    private List<EffectDefinition> effects;

    public EffectBrowserGUI(Player player, EffectManager effectManager, EffectEngine engine,
            ChatPromptManager chatPromptManager, LangManager langManager) {
        super(player, langManager.component("browser.title"), SIZE);
        this.effectManager = effectManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
        this.langManager = langManager;
        this.effects = List.copyOf(effectManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < effects.size() && i < 36; i++) {

            EffectDefinition effect = effects.get(i);

            List<Component> lore = new ArrayList<>();

            for (String line : wrap(effect.description(), DESCRIPTION_WIDTH)) {
                lore.add(Component.text(line, NamedTextColor.GRAY));
            }

            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }

            lore.add(langManager.component("browser.item_lore_id", "id", effect.id()));
            lore.add(langManager.component("browser.item_lore_steps", "count", effect.steps().size()));
            lore.add(langManager.component("browser.item_lore_edit"));
            lore.add(langManager.component("browser.item_lore_test"));

            setItem(i, new ItemBuilder(iconOf(effect))
                    .setName(ComponentUtils.parse(effect.displayName()))
                    .setLore(lore)
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(langManager.component("browser.new_effect_name"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(langManager.raw("browser.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < effects.size() && slot < 36 && event.isRightClick()) {
            engine.play(effects.get(slot), EffectContext.of(player));
            return;
        }

        if (slot < effects.size() && slot < 36) {
            new EffectEditorGUI(player, effects.get(slot), effectManager, engine, chatPromptManager, langManager,
                    this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, langManager.raw("browser.new_effect_prompt"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (effectManager.exists(id)) {
                langManager.send(player, "browser.id_exists");
                reopen();
                return;
            }

            EffectDefinition effect = new EffectDefinition(id, id, "", List.of());
            effectManager.save(effect);
            reopen();
        });
    }

    /** El {@code icon} del YAML si es un material válido; si no, polvo de blaze. */
    private static Material iconOf(EffectDefinition effect) {

        Material material = effect.icon() == null ? null : Material.matchMaterial(effect.icon());
        return material != null && material.isItem() && !material.isAir() ? material : Material.BLAZE_POWDER;
    }

    /** Parte la descripción en líneas cortas para el lore (sin esto sale una sola línea kilométrica). */
    static List<String> wrap(String text, int width) {

        List<String> lines = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return lines;
        }

        StringBuilder line = new StringBuilder();

        for (String word : text.trim().split("\\s+")) {

            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }

            if (line.length() > 0) {
                line.append(' ');
            }

            line.append(word);
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }

        return lines;
    }

    private void reopen() {
        this.effects = List.copyOf(effectManager.getAll());
        open();
    }

}
