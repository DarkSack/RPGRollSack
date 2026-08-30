package com.sack.rpgroll.traps.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.core.TrapManager;
import com.sack.rpgroll.traps.core.TrapTrigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Campos básicos: nombre, ícono, trigger, radio, cooldown y cargas. Click
 * izquierdo suma, click derecho resta, shift multiplica el paso — mismo
 * lenguaje de interacción que el resto de editores numéricos del repo
 * (ej. DungeonInfoEditorGUI).
 */
public class TrapInfoEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int TRIGGER_SLOT = 12;
    private static final int RADIUS_SLOT = 13;
    private static final int COOLDOWN_SLOT = 14;
    private static final int CHARGES_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final ChatPromptManager chatPromptManager;
    private final Consumer<TrapDefinition> onSave;
    private final Runnable onBack;
    private TrapDefinition current;

    public TrapInfoEditorGUI(Player player, TrapDefinition current, ChatPromptManager chatPromptManager,
            Consumer<TrapDefinition> onSave, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.info.title", "id", current.id()), SIZE);
        this.current = current;
        this.chatPromptManager = chatPromptManager;
        this.onSave = onSave;
        this.onBack = onBack;
    }

    private LangManager lang() {
        return chatPromptManager.lang();
    }

    private void replace(TrapDefinition updated) {
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

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang().component("gui.info.name", "value", current.displayName()))
                .setLore(lang().component("gui.common.click_to_write"))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(current.icon())
                .setName(lang().component("gui.info.icon", "value", current.icon().name()))
                .setLore(lang().component("gui.common.click_to_write"))
                .build());

        setItem(TRIGGER_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang().component("gui.info.trigger", "value", current.trigger().name()))
                .setLore(lang().component("gui.info.trigger_hint"))
                .build());

        setItem(RADIUS_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(lang().component("gui.info.radius", "value", current.radius()))
                .setLore(lang().component("gui.info.updown_hint"))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang().component("gui.info.cooldown", "value", current.cooldownMillis() / 1000))
                .setLore(lang().component("gui.info.cooldown_hint"))
                .build());

        setItem(CHARGES_SLOT, new ItemBuilder(Material.TOTEM_OF_UNDYING)
                .setName(lang().component("gui.info.charges", "value",
                        current.hasInfiniteCharges() ? lang().raw("gui.info.charges_infinite")
                                : String.valueOf(current.charges())))
                .setLore(lang().component("gui.info.updown_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "gui.info.prompt_name", value -> replace(withDisplayName(value)));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "gui.info.prompt_icon", value -> {
                try {
                    replace(withIcon(Material.valueOf(value.trim().toUpperCase(Locale.ROOT))));
                } catch (IllegalArgumentException e) {
                    lang().send(player, "gui.info.invalid_material");
                }
            });
            return;
        }

        if (slot == TRIGGER_SLOT) {
            TrapTrigger[] values = TrapTrigger.values();
            TrapTrigger next = values[(current.trigger().ordinal() + 1) % values.length];
            replace(withTrigger(next));
            return;
        }

        if (slot == RADIUS_SLOT) {
            double step = click.isShiftClick() ? 2.0 : 0.5;
            double delta = click.isRightClick() ? -step : step;
            replace(withRadius(Math.max(0.5, current.radius() + delta)));
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            long stepMillis = click.isShiftClick() ? 30_000 : 1_000;
            long delta = click.isRightClick() ? -stepMillis : stepMillis;
            replace(withCooldown(Math.max(0, current.cooldownMillis() + delta)));
            return;
        }

        if (slot == CHARGES_SLOT) {
            if (click.isRightClick()) {
                replace(withCharges(current.hasInfiniteCharges() ? -1 : Math.max(-1, current.charges() - 1)));
            } else {
                replace(withCharges(current.hasInfiniteCharges() ? 1 : current.charges() + 1));
            }
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private TrapDefinition withDisplayName(String value) {
        return new TrapDefinition(current.id(), value, current.description(), current.icon(), current.trigger(),
                current.triggerParams(), current.radius(), current.conditions(), current.actions(),
                current.cooldownMillis(), current.charges(), current.chain(), current.block(), current.disguise());
    }

    private TrapDefinition withIcon(Material icon) {
        return new TrapDefinition(current.id(), current.displayName(), current.description(), icon,
                current.trigger(), current.triggerParams(), current.radius(), current.conditions(),
                current.actions(), current.cooldownMillis(), current.charges(), current.chain(), current.block(),
                current.disguise());
    }

    private TrapDefinition withTrigger(TrapTrigger trigger) {
        return new TrapDefinition(current.id(), current.displayName(), current.description(), current.icon(),
                trigger, current.triggerParams(), current.radius(), current.conditions(), current.actions(),
                current.cooldownMillis(), current.charges(), current.chain(), current.block(), current.disguise());
    }

    private TrapDefinition withRadius(double radius) {
        return new TrapDefinition(current.id(), current.displayName(), current.description(), current.icon(),
                current.trigger(), current.triggerParams(), radius, current.conditions(), current.actions(),
                current.cooldownMillis(), current.charges(), current.chain(), current.block(), current.disguise());
    }

    private TrapDefinition withCooldown(long cooldownMillis) {
        return new TrapDefinition(current.id(), current.displayName(), current.description(), current.icon(),
                current.trigger(), current.triggerParams(), current.radius(), current.conditions(),
                current.actions(), cooldownMillis, current.charges(), current.chain(), current.block(),
                current.disguise());
    }

    private TrapDefinition withCharges(int charges) {
        return new TrapDefinition(current.id(), current.displayName(), current.description(), current.icon(),
                current.trigger(), current.triggerParams(), current.radius(), current.conditions(),
                current.actions(), current.cooldownMillis(), charges, current.chain(), current.block(),
                current.disguise());
    }

}
