package com.sack.rpgroll.sackeffects.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.sackeffects.core.EffectDefinition;
import com.sack.rpgroll.sackeffects.core.EffectManager;
import com.sack.rpgroll.sackeffects.engine.EffectEngine;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class EffectBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

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

            setItem(i, new ItemBuilder(Material.BLAZE_POWDER)
                    .setName(Component.text(effect.displayName(), NamedTextColor.AQUA))
                    .setLore(langManager.component("browser.item_lore_id", "id", effect.id()),
                            langManager.component("browser.item_lore_steps", "count", effect.steps().size()),
                            langManager.component("browser.item_lore_edit"))
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

    private void reopen() {
        this.effects = List.copyOf(effectManager.getAll());
        open();
    }

}
