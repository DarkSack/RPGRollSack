package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.event.WorkerEventDefinition;
import com.sack.rpgroll.workers.core.event.WorkerEventManager;
import com.sack.rpgroll.workers.core.event.WorkerEventType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

public class WorkerEventEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int TYPE_SLOT = 11;
    private static final int CHANCE_SLOT = 12;
    private static final int DURATION_SLOT = 13;
    private static final int HAPPINESS_SLOT = 14;
    private static final int ENERGY_SLOT = 15;
    private static final int HEALTH_SLOT = 16;
    private static final int SPEED_SLOT = 17;
    private static final int BACK_SLOT = 40;

    private final WorkerEventManager eventManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private WorkerEventDefinition current;

    public WorkerEventEditorGUI(Player player, WorkerEventDefinition event, WorkerEventManager eventManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.event.editor.title", "id", event.id()),
                NamedTextColor.GOLD), SIZE);
        this.current = event;
        this.eventManager = eventManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(WorkerEventDefinition updated) {
        current = updated;
        eventManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text(chatPromptManager.lang().raw("gui.skill.editor.name", "name", current.displayName()),
                        NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text(chatPromptManager.lang().raw("gui.profession.editor.description"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? chatPromptManager.lang().raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text(chatPromptManager.lang().raw("gui.event.editor.type", "type", current.type()),
                        NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.profession.editor.rotate_hint"), NamedTextColor.GRAY))
                .build());

        setItem(CHANCE_SLOT, new ItemBuilder(Material.SLIME_BALL)
                .setName(Component.text(current.chance() > 0
                        ? chatPromptManager.lang().raw("gui.event.editor.chance_own", "percent",
                                String.format(Locale.ROOT, "%.1f", current.chance() * 100))
                        : chatPromptManager.lang().raw("gui.event.editor.chance_global"), NamedTextColor.GREEN))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.event.editor.chance_hint"), NamedTextColor.GRAY))
                .build());

        setItem(DURATION_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text(chatPromptManager.lang().raw("gui.event.editor.duration", "ticks",
                        current.durationTicks()), NamedTextColor.WHITE))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.event.editor.duration_hint"), NamedTextColor.GRAY))
                .build());

        setItem(HAPPINESS_SLOT, new ItemBuilder(Material.MAGENTA_DYE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.event.editor.happiness", "value",
                        current.happinessDelta()), NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.event.editor.stat_hint"), NamedTextColor.GRAY))
                .build());

        setItem(ENERGY_SLOT, new ItemBuilder(Material.SUGAR)
                .setName(Component.text(chatPromptManager.lang().raw("gui.event.editor.energy", "value",
                        current.energyDelta()), NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.event.editor.stat_hint"), NamedTextColor.GRAY))
                .build());

        setItem(HEALTH_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.event.editor.health", "value",
                        current.healthDelta()), NamedTextColor.RED))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.event.editor.stat_hint"), NamedTextColor.GRAY))
                .build());

        setItem(SPEED_SLOT, new ItemBuilder(Material.FEATHER)
                .setName(Component.text(
                        chatPromptManager.lang().raw("gui.event.editor.speed", "percent",
                                String.format(Locale.ROOT, "%.0f", current.workSpeedMultiplierWhileActive() * 100)),
                        NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.event.editor.speed_hint"),
                        NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"),
                    value -> replace(new WorkerEventDefinition(
                    current.id(), value, current.description(), current.type(), current.chance(),
                    current.durationTicks(), current.happinessDelta(), current.energyDelta(), current.healthDelta(),
                    current.workSpeedMultiplierWhileActive())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"),
                    value -> replace(new WorkerEventDefinition(
                    current.id(), current.displayName(), value, current.type(), current.chance(),
                    current.durationTicks(), current.happinessDelta(), current.energyDelta(), current.healthDelta(),
                    current.workSpeedMultiplierWhileActive())));
        } else if (slot == TYPE_SLOT) {
            WorkerEventType[] values = WorkerEventType.values();
            WorkerEventType next = values[(current.type().ordinal() + 1) % values.length];
            replace(new WorkerEventDefinition(current.id(), current.displayName(), current.description(), next,
                    current.chance(), current.durationTicks(), current.happinessDelta(), current.energyDelta(),
                    current.healthDelta(), current.workSpeedMultiplierWhileActive()));
        } else if (slot == CHANCE_SLOT) {
            replace(new WorkerEventDefinition(current.id(), current.displayName(), current.description(), current.type(),
                    Math.max(0, Math.min(1, current.chance() + sign * 0.05)), current.durationTicks(),
                    current.happinessDelta(), current.energyDelta(), current.healthDelta(),
                    current.workSpeedMultiplierWhileActive()));
        } else if (slot == DURATION_SLOT) {
            replace(new WorkerEventDefinition(current.id(), current.displayName(), current.description(), current.type(),
                    current.chance(), Math.max(0, current.durationTicks() + sign * 1200L), current.happinessDelta(),
                    current.energyDelta(), current.healthDelta(), current.workSpeedMultiplierWhileActive()));
        } else if (slot == HAPPINESS_SLOT) {
            replace(new WorkerEventDefinition(current.id(), current.displayName(), current.description(), current.type(),
                    current.chance(), current.durationTicks(), current.happinessDelta() + sign * 5, current.energyDelta(),
                    current.healthDelta(), current.workSpeedMultiplierWhileActive()));
        } else if (slot == ENERGY_SLOT) {
            replace(new WorkerEventDefinition(current.id(), current.displayName(), current.description(), current.type(),
                    current.chance(), current.durationTicks(), current.happinessDelta(), current.energyDelta() + sign * 5,
                    current.healthDelta(), current.workSpeedMultiplierWhileActive()));
        } else if (slot == HEALTH_SLOT) {
            replace(new WorkerEventDefinition(current.id(), current.displayName(), current.description(), current.type(),
                    current.chance(), current.durationTicks(), current.happinessDelta(), current.energyDelta(),
                    current.healthDelta() + sign * 5, current.workSpeedMultiplierWhileActive()));
        } else if (slot == SPEED_SLOT) {
            replace(new WorkerEventDefinition(current.id(), current.displayName(), current.description(), current.type(),
                    current.chance(), current.durationTicks(), current.happinessDelta(), current.energyDelta(),
                    current.healthDelta(), Math.max(0, Math.min(1, current.workSpeedMultiplierWhileActive() + sign * 0.1))));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
