package com.sack.rpgroll.traps.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.core.TrapBlockConfig;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;
import java.util.function.Consumer;

/** Configuración de bloque protegido (material, rompible, llave requerida, inmunidades). */
public class TrapBlockKeyEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int MATERIAL_SLOT = 10;
    private static final int BREAKABLE_SLOT = 11;
    private static final int KEY_SLOT = 12;
    private static final int BREAK_TIME_SLOT = 13;
    private static final int EXPLOSION_IMMUNE_SLOT = 14;
    private static final int PISTON_IMMUNE_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final ChatPromptManager chatPromptManager;
    private final Consumer<TrapBlockConfig> onSave;
    private final Runnable onBack;
    private TrapBlockConfig current;

    public TrapBlockKeyEditorGUI(Player player, TrapBlockConfig current, ChatPromptManager chatPromptManager,
            Consumer<TrapBlockConfig> onSave, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.block.title"), SIZE);
        this.current = current;
        this.chatPromptManager = chatPromptManager;
        this.onSave = onSave;
        this.onBack = onBack;
    }

    private LangManager lang() {
        return chatPromptManager.lang();
    }

    private void replace(TrapBlockConfig updated) {
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

        setItem(MATERIAL_SLOT, new ItemBuilder(current.material() == null ? Material.BARRIER : current.material())
                .setName(lang().component("gui.block.material", "value",
                        current.material() == null ? lang().raw("gui.block.material_none") : current.material().name()))
                .setLore(lang().component("gui.common.click_to_write"))
                .build());

        setItem(BREAKABLE_SLOT, new ItemBuilder(current.breakable() ? Material.LIME_DYE : Material.RED_DYE)
                .setName(lang().component("gui.block.breakable", "value", current.breakable()))
                .setLore(lang().component("gui.common.click_to_toggle"))
                .build());

        setItem(KEY_SLOT, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .setName(lang().component("gui.block.key", "value",
                        current.requiresKey() ? current.requiredItemId() : lang().raw("gui.block.key_none")))
                .setLore(lang().component("gui.common.click_to_write"), lang().component("gui.block.key_shift_clear"))
                .build());

        setItem(BREAK_TIME_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang().component("gui.block.break_time", "value", current.breakTimeSeconds()))
                .setLore(lang().component("gui.info.updown_hint"))
                .build());

        setItem(EXPLOSION_IMMUNE_SLOT, new ItemBuilder(current.explosionImmune() ? Material.LIME_DYE : Material.RED_DYE)
                .setName(lang().component("gui.block.explosion_immune", "value", current.explosionImmune()))
                .setLore(lang().component("gui.common.click_to_toggle"))
                .build());

        setItem(PISTON_IMMUNE_SLOT, new ItemBuilder(current.pistonImmune() ? Material.LIME_DYE : Material.RED_DYE)
                .setName(lang().component("gui.block.piston_immune", "value", current.pistonImmune()))
                .setLore(lang().component("gui.common.click_to_toggle"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == MATERIAL_SLOT) {
            chatPromptManager.prompt(player, "gui.block.prompt_material", value -> {
                try {
                    Material material = Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
                    replace(new TrapBlockConfig(material, current.breakable(), current.requiredItemId(),
                            current.breakTimeSeconds(), current.explosionImmune(), current.pistonImmune()));
                } catch (IllegalArgumentException e) {
                    lang().send(player, "gui.info.invalid_material");
                }
            });
            return;
        }

        if (slot == BREAKABLE_SLOT) {
            replace(new TrapBlockConfig(current.material(), !current.breakable(), current.requiredItemId(),
                    current.breakTimeSeconds(), current.explosionImmune(), current.pistonImmune()));
            return;
        }

        if (slot == KEY_SLOT) {
            if (event.isShiftClick()) {
                replace(new TrapBlockConfig(current.material(), current.breakable(), null,
                        current.breakTimeSeconds(), current.explosionImmune(), current.pistonImmune()));
                return;
            }
            chatPromptManager.prompt(player, "gui.block.prompt_key", value -> replace(
                    new TrapBlockConfig(current.material(), current.breakable(), value.trim(),
                            current.breakTimeSeconds(), current.explosionImmune(), current.pistonImmune())));
            return;
        }

        if (slot == BREAK_TIME_SLOT) {
            int delta = event.isRightClick() ? -1 : 1;
            replace(new TrapBlockConfig(current.material(), current.breakable(), current.requiredItemId(),
                    Math.max(0, current.breakTimeSeconds() + delta), current.explosionImmune(),
                    current.pistonImmune()));
            return;
        }

        if (slot == EXPLOSION_IMMUNE_SLOT) {
            replace(new TrapBlockConfig(current.material(), current.breakable(), current.requiredItemId(),
                    current.breakTimeSeconds(), !current.explosionImmune(), current.pistonImmune()));
            return;
        }

        if (slot == PISTON_IMMUNE_SLOT) {
            replace(new TrapBlockConfig(current.material(), current.breakable(), current.requiredItemId(),
                    current.breakTimeSeconds(), current.explosionImmune(), !current.pistonImmune()));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
