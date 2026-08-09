package com.sack.rpgroll.sackeffects.gui;

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
    private List<EffectDefinition> effects;

    public EffectBrowserGUI(Player player, EffectManager effectManager, EffectEngine engine,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Efectos RPGRoll", NamedTextColor.GOLD), SIZE);
        this.effectManager = effectManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
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
                    .setLore(Component.text("id: " + effect.id(), NamedTextColor.GRAY),
                            Component.text(effect.steps().size() + " step(s)", NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear efecto nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < effects.size() && slot < 36) {
            new EffectEditorGUI(player, effects.get(slot), effectManager, engine, chatPromptManager, this::reopen)
                    .open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo efecto:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (effectManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un efecto con ese id.", NamedTextColor.RED));
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
