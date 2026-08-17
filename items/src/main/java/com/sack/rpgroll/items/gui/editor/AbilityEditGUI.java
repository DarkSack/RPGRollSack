package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemAbility;
import com.sack.rpgroll.items.core.ItemAction;
import com.sack.rpgroll.items.core.ItemTrigger;
import com.sack.rpgroll.items.util.DurationParser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Detalle de una habilidad: nombre, pasiva/activa, trigger, cooldown, condiciones y sus acciones. */
public class AbilityEditGUI extends InventoryGUI {

    private static final int SIZE = 36;
    private static final int RENAME_SLOT = 10;
    private static final int PASSIVE_SLOT = 11;
    private static final int TRIGGER_SLOT = 12;
    private static final int COOLDOWN_SLOT = 13;
    private static final int CONDITIONS_SLOT = 14;
    private static final int ACTIONS_SLOT = 15;
    private static final int BACK_SLOT = 31;

    private final EditorSession session;
    private final LangManager lang;
    private final int index;
    private final Runnable onBack;

    public AbilityEditGUI(Player player, EditorSession session, int index, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.ability_edit.title",
                "id", session.abilities.get(index).id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
        this.index = index;
        this.onBack = onBack;
    }

    private ItemAbility ability() {
        return session.abilities.get(index);
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 27; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        ItemAbility ability = ability();

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("editor.ability_edit.name_label", "name", ability.displayName()))
                .setLore(lang.component("editor.ability_edit.click_rename"))
                .build());

        setItem(PASSIVE_SLOT, new ItemBuilder(ability.passive() ? Material.BEACON : Material.BLAZE_POWDER)
                .setName(lang.component("editor.ability_edit.type_label", "value", ability.passive()
                        ? lang.raw("editor.ability_edit.type_passive") : lang.raw("editor.ability_edit.type_active")))
                .setLore(lang.component("editor.ability_edit.click_toggle"))
                .build());

        setItem(TRIGGER_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang.component("editor.ability_edit.trigger_label",
                        "value", ability.trigger() == null ? lang.raw("editor.ability_edit.trigger_none") : ability.trigger()))
                .setLore(lang.component("editor.ability_edit.trigger_hint"))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("editor.ability_edit.cooldown_label", "seconds", ability.cooldownMillis() / 1000))
                .setLore(lang.component("editor.ability_edit.click_write"))
                .build());

        List<Component> conditionsLore = new ArrayList<>();
        if (ability.conditions().isEmpty()) {
            conditionsLore.add(lang.component("editor.ability_edit.conditions_none"));
        } else {
            for (String condition : ability.conditions()) {
                conditionsLore.add(Component.text(condition, NamedTextColor.GRAY));
            }
        }

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("editor.ability_edit.conditions_label", "count", ability.conditions().size()))
                .setLore(conditionsLore)
                .build());

        setItem(ACTIONS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(lang.component("editor.ability_edit.actions_label", "count", ability.actions().size()))
                .setLore(lang.component("editor.ability_edit.click_edit"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        switch (event.getSlot()) {
            case RENAME_SLOT -> promptRename();
            case PASSIVE_SLOT -> togglePassive();
            case TRIGGER_SLOT -> cycleTrigger();
            case COOLDOWN_SLOT -> promptCooldown();
            case CONDITIONS_SLOT -> promptConditions();
            case ACTIONS_SLOT -> openActions();
            case BACK_SLOT -> onBack.run();
            default -> {
            }
        }
    }

    private void replace(ItemAbility updated) {
        session.abilities.set(index, updated);
        build();
    }

    private void promptRename() {
        session.chatPromptManager.prompt(player, lang.raw("editor.ability_edit.prompt_rename"), value -> {
            ItemAbility a = ability();
            replace(new ItemAbility(a.id(), value, a.passive(), a.trigger(), a.cooldownMillis(), a.conditions(),
                    a.actions()));
        });
    }

    private void togglePassive() {
        ItemAbility a = ability();
        replace(new ItemAbility(a.id(), a.displayName(), !a.passive(), a.trigger(), a.cooldownMillis(),
                a.conditions(), a.actions()));
    }

    private void cycleTrigger() {

        ItemAbility a = ability();
        ItemTrigger[] values = ItemTrigger.values();

        ItemTrigger next;
        if (a.trigger() == null) {
            next = values[0];
        } else {
            int currentIndex = a.trigger().ordinal();
            next = currentIndex + 1 < values.length ? values[currentIndex + 1] : null;
        }

        replace(new ItemAbility(a.id(), a.displayName(), a.passive(), next, a.cooldownMillis(), a.conditions(),
                a.actions()));
    }

    private void promptCooldown() {
        session.chatPromptManager.prompt(player, lang.raw("editor.ability_edit.prompt_cooldown"), value -> {
            ItemAbility a = ability();
            long millis = DurationParser.parseMillis(value.trim());
            replace(new ItemAbility(a.id(), a.displayName(), a.passive(), a.trigger(), millis, a.conditions(),
                    a.actions()));
        });
    }

    private void promptConditions() {
        session.chatPromptManager.prompt(player, lang.raw("editor.ability_edit.prompt_conditions"), value -> {

            ItemAbility a = ability();

            List<String> conditions = value.isBlank()
                    ? List.of()
                    : List.of(value.split(";")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();

            replace(new ItemAbility(a.id(), a.displayName(), a.passive(), a.trigger(), a.cooldownMillis(),
                    conditions, a.actions()));
        });
    }

    private void openActions() {

        List<ItemAction> workingActions = new ArrayList<>(ability().actions());

        new ActionListEditorGUI(player, lang.raw("editor.ability_edit.actions_of", "ability", ability().displayName()),
                workingActions, session.chatPromptManager, () -> {

                    ItemAbility a = ability();
                    session.abilities.set(index, new ItemAbility(a.id(), a.displayName(), a.passive(), a.trigger(),
                            a.cooldownMillis(), a.conditions(), workingActions));

                    new AbilityEditGUI(player, session, index, onBack).open();
                }).open();
    }

}
