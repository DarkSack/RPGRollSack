package com.sack.rpgroll.traps.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.core.TrapDisguiseConfig;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Camuflaje v1 (swap de bloque real, ver TrapDisguiseConfig) — NO es
 * camuflaje por-jugador vía paquetes, eso quedó fuera de alcance.
 */
public class TrapDisguiseEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int MODE_SLOT = 11;
    private static final int VISIBLE_SLOT = 13;
    private static final int REVEALED_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final ChatPromptManager chatPromptManager;
    private final Consumer<TrapDisguiseConfig> onSave;
    private final Runnable onBack;
    private TrapDisguiseConfig current;

    public TrapDisguiseEditorGUI(Player player, TrapDisguiseConfig current, ChatPromptManager chatPromptManager,
            Consumer<TrapDisguiseConfig> onSave, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.disguise.title"), SIZE);
        this.current = current;
        this.chatPromptManager = chatPromptManager;
        this.onSave = onSave;
        this.onBack = onBack;
    }

    private LangManager lang() {
        return chatPromptManager.lang();
    }

    private void replace(TrapDisguiseConfig updated) {
        this.current = updated;
        onSave.accept(updated);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(MODE_SLOT, new ItemBuilder(Material.LEATHER)
                .setName(lang().component("gui.disguise.mode", "value", current.mode().name()))
                .setLore(lang().component("gui.common.click_to_toggle"))
                .build());

        setItem(VISIBLE_SLOT, new ItemBuilder(current.visibleMaterial() == null ? Material.BARRIER
                        : current.visibleMaterial())
                .setName(lang().component("gui.disguise.visible", "value",
                        current.visibleMaterial() == null ? lang().raw("gui.block.material_none")
                                : current.visibleMaterial().name()))
                .setLore(lang().component("gui.common.click_to_write"))
                .build());

        setItem(REVEALED_SLOT, new ItemBuilder(current.revealedMaterial() == null ? Material.BARRIER
                        : current.revealedMaterial())
                .setName(lang().component("gui.disguise.revealed", "value",
                        current.revealedMaterial() == null ? lang().raw("gui.block.material_none")
                                : current.revealedMaterial().name()))
                .setLore(lang().component("gui.common.click_to_write"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == MODE_SLOT) {
            TrapDisguiseConfig.DisguiseMode[] values = TrapDisguiseConfig.DisguiseMode.values();
            var next = values[(current.mode().ordinal() + 1) % values.length];
            replace(new TrapDisguiseConfig(next, current.visibleMaterial(), current.revealedMaterial()));
            return;
        }

        if (slot == VISIBLE_SLOT) {
            chatPromptManager.prompt(player, "gui.disguise.prompt_visible", value -> {
                try {
                    Material material = Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
                    replace(new TrapDisguiseConfig(current.mode(), material, current.revealedMaterial()));
                } catch (IllegalArgumentException e) {
                    lang().send(player, "gui.info.invalid_material");
                }
            });
            return;
        }

        if (slot == REVEALED_SLOT) {
            chatPromptManager.prompt(player, "gui.disguise.prompt_revealed", value -> {
                try {
                    Material material = Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
                    replace(new TrapDisguiseConfig(current.mode(), current.visibleMaterial(), material));
                } catch (IllegalArgumentException e) {
                    lang().send(player, "gui.info.invalid_material");
                }
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
